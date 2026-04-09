package com.irerin.travelan.board.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    @Test
    void removesScriptTag() {
        String dirty = "<p>hello</p><script>alert('x')</script>";
        assertThat(HtmlSanitizer.sanitize(dirty)).doesNotContain("<script>");
    }

    @Test
    void removesOnclickAttribute() {
        String dirty = "<a href=\"https://example.com\" onclick=\"hack()\">link</a>";
        String clean = HtmlSanitizer.sanitize(dirty);
        assertThat(clean).doesNotContain("onclick");
        assertThat(clean).contains("href=\"https://example.com\"");
    }

    @Test
    void removesIframe() {
        String dirty = "<iframe src=\"http://evil\"></iframe><p>ok</p>";
        assertThat(HtmlSanitizer.sanitize(dirty)).doesNotContain("<iframe");
    }

    @Test
    void preservesInlineFormatting() {
        String html = "<p><strong>bold</strong> <em>italic</em></p>";
        assertThat(HtmlSanitizer.sanitize(html)).contains("<strong>").contains("<em>");
    }

    @Test
    void returnsNullForNull() {
        assertThat(HtmlSanitizer.sanitize(null)).isNull();
    }
}
