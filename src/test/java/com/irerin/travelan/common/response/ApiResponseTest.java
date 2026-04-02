package com.irerin.travelan.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ApiResponseTest {

    // ── 기존 정적 팩토리 ─────────────────────────────────────────────────────

    @Test
    void ok_data_성공응답() {
        ApiResponse<String> response = ApiResponse.ok("hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getPage()).isNull();
        assertThat(response.getSearch()).isNull();
        assertThat(response.getError()).isNull();
    }

    @Test
    void error_에러응답() {
        ErrorResponse error = ErrorResponse.of("NOT_FOUND", "리소스 없음");

        ApiResponse<Void> response = ApiResponse.error(error);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getError().getCode()).isEqualTo("NOT_FOUND");
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    @Test
    void builder_data와_search와_page_모두_포함() {
        PageImpl<String> page = new PageImpl<>(List.of("a"), PageRequest.of(0, 20), 1);
        PageMeta pageMeta = PageMeta.from(page, 1);
        Object search = new Object();

        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
            .data(List.of("a"))
            .page(pageMeta)
            .search(search)
            .build();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).containsExactly("a");
        assertThat(response.getPage()).isEqualTo(pageMeta);
        assertThat(response.getSearch()).isEqualTo(search);
        assertThat(response.getError()).isNull();
    }

    @Test
    void builder_search만_포함() {
        String searchCriteria = "keyword";

        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
            .data(List.of("result"))
            .search(searchCriteria)
            .build();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSearch()).isEqualTo("keyword");
        assertThat(response.getPage()).isNull();
    }

    @Test
    void builder_ofPage와_search_조합() {
        PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 10), 2);
        String search = "검색어";

        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
            .data(page.getContent())
            .page(PageMeta.from(page, 1))
            .search(search)
            .build();

        assertThat(response.getData()).hasSize(2);
        assertThat(response.getPage().getTotalElements()).isEqualTo(2);
        assertThat(response.getSearch()).isEqualTo("검색어");
    }

    @Test
    void builder_error_설정시_success_false() {
        ErrorResponse error = ErrorResponse.of("FORBIDDEN", "권한 없음");

        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .error(error)
            .build();

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError().getCode()).isEqualTo("FORBIDDEN");
        assertThat(response.getData()).isNull();
        assertThat(response.getSearch()).isNull();
    }

    @Test
    void builder_기본값은_success_true() {
        ApiResponse<String> response = ApiResponse.<String>builder()
            .data("test")
            .build();

        assertThat(response.isSuccess()).isTrue();
    }

    // ── Builder data(Page) ───────────────────────────────────────────────────

    @Test
    void builder_Page_넘기면_content와_page메타_자동_설정() {
        // pageSize=2, total=5 → offset(0)+pageSize(2) <= total(5) → 보정 없이 total=5 유지
        PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5);

        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
            .data(page)
            .build();

        assertThat(response.getData()).containsExactly("a", "b");
        assertThat(response.getPage().getPage()).isEqualTo(1);  // getNumber()(0) + 1
        assertThat(response.getPage().getTotalElements()).isEqualTo(5);
        assertThat(response.getPage().getTotalPages()).isEqualTo(3);
    }

    @Test
    void builder_Page와_search와_meta_조합() {
        PageImpl<String> page = new PageImpl<>(List.of("x"), PageRequest.of(0, 20), 1);

        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
            .data(page)
            .search("검색어")
            .meta("totalCount", 1L)
            .build();

        assertThat(response.getData()).containsExactly("x");
        assertThat(response.getPage()).isNotNull();
        assertThat(response.getSearch()).isEqualTo("검색어");
        assertThat(response.getMeta()).containsEntry("totalCount", 1L);
    }

    // ── Builder meta ─────────────────────────────────────────────────────────

    @Test
    void builder_meta_단일_키값_포함() {
        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
            .data(List.of("a"))
            .meta("totalCount", 100L)
            .build();

        assertThat(response.getMeta()).containsEntry("totalCount", 100L);
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void builder_meta_여러_키값_포함() {
        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
            .data(List.of("a"))
            .meta("totalCount", 100L)
            .meta("ratingAverage", 4.5)
            .meta("replyCount", 20)
            .build();

        assertThat(response.getMeta())
            .containsEntry("totalCount", 100L)
            .containsEntry("ratingAverage", 4.5)
            .containsEntry("replyCount", 20);
    }

    @Test
    void builder_meta_없으면_null() {
        ApiResponse<String> response = ApiResponse.<String>builder()
            .data("test")
            .build();

        assertThat(response.getMeta()).isNull();
    }
}
