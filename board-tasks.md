# 게시판 (Board) - Tasks

## Phase 0 — 사전 결정 (완료)
- [x] 본문 포맷 → **Sanitized HTML** (Jsoup `Safelist.relaxed()`)
- [x] 초기 Region 시드 → **16개**: 국내(서울/경기/인천/강원/충청/전라/경상/제주) + 인기 해외(일본/중국/베트남/태국/필리핀/대만/미국) + 기타 해외
- [x] 이미지 저장 경로 → **`./uploads/posts/{userId}/{yyyy}/{MM}/{uuid}.{ext}`**

---

## Phase 1 — Region
- [ ] Region 엔티티 작성 (`@Builder` private + `of(...)`)
- [ ] RegionRepository
- [ ] RegionRepository 테스트 (RED → GREEN)
- [ ] RegionService — `findAllActive()`
- [ ] RegionService 테스트
- [ ] RegionResponse DTO
- [ ] RegionController — `GET /api/v1/regions`
- [ ] RegionControllerTest (MockMvc, 비회원 200)
- [ ] SecurityConfig: `/api/v1/regions/**` GET 허용
- [ ] 시드 데이터 추가 (data.sql 또는 마이그레이션) — 16개 Region (국내 8 → 인기 해외 7 → 기타 해외 1 순으로 displayOrder 부여)
- [ ] 통합 테스트: 비회원 카테고리 조회 (16개 정렬 검증)

---

## Phase 2 — Post CRUD
### 엔티티 / 리포지토리
- [ ] PostStatus enum (PUBLISHED, DELETED, BLOCKED)
- [ ] Post 엔티티 (Region, User FK, soft delete)
- [ ] Post 엔티티 테스트 (`of`, `update`, `delete`, 권한 체크 메서드)
- [ ] PostRepository — `findByRegionAndStatus(...)`, `findByIdAndStatusNot(...)`
- [ ] PostRepository 테스트

### HTML Sanitizer
- [ ] HtmlSanitizer 유틸 작성 (Jsoup `Safelist.relaxed()` 기반)
- [ ] HtmlSanitizer 테스트 (`<script>`, `onclick=`, `<iframe>` 제거 검증 / 정상 인라인 서식 유지 검증)

### 서비스
- [ ] CreatePostCommand / UpdatePostCommand
- [ ] PostService.create / get / list / update / delete (create·update에서 본문 sanitize 적용)
- [ ] PostService 테스트 (작성자 권한, 운영자 권한, 탈퇴 사용자 케이스, sanitize 적용 케이스)

### 컨트롤러
- [ ] CreatePostRequest / UpdatePostRequest (웹 DTO) + Command 변환
- [ ] PostSummaryResponse / PostDetailResponse
- [ ] PostController — 5개 엔드포인트
- [ ] PostControllerTest (각 권한 시나리오)
- [ ] SecurityConfig: 조회 GET 비회원 허용, 나머지 인증

### 통합
- [ ] 통합 테스트: 비회원 조회 → 회원 작성 → 작성자 수정 → 운영자 삭제
- [ ] 통합 테스트: 탈퇴 사용자 작성 글이 목록/상세에서 정상 노출되는지

---

## Phase 3 — 이미지 첨부
- [ ] FileStorage 인터페이스 (`store(userId, file)`, `delete(path)`, `toUrl(path)`)
- [ ] LocalFileStorage 구현 — 경로 규약 `./uploads/posts/{userId}/{yyyy}/{MM}/{uuid}.{ext}`
- [ ] LocalFileStorage 테스트 (임시 디렉터리, 경로 규약 검증)
- [ ] PostImage 엔티티
- [ ] PostImageRepository
- [ ] ImageUploadService — 검증 (확장자 화이트리스트, Content-Type, 크기, 개수)
- [ ] ImageUploadService 테스트 (실패 케이스 포함: 잘못된 확장자, Content-Type 불일치, 초과 크기)
- [ ] PostImageResponse DTO
- [ ] ImageController — `POST /api/v1/posts/images`
- [ ] ImageControllerTest (multipart)
- [ ] Post 작성/수정 시 `imageIds` 연결 로직
- [ ] 정적 리소스 서빙 설정 (WebMvcConfigurer)
- [ ] 통합 테스트: 이미지 업로드 → 게시글 작성 → 비회원 조회 시 URL 포함

---

## Phase 4 — 신고
- [ ] ReportReason enum
- [ ] PostReport 엔티티
- [ ] PostReportRepository — `existsByPostIdAndReporterId`
- [ ] PostReportRepository 테스트
- [ ] ReportPostCommand
- [ ] PostReportService — 자기글 신고 금지, 중복 금지
- [ ] PostReportService 테스트
- [ ] CreateReportRequest / ReportResponse
- [ ] PostReportController — `POST /api/v1/posts/{id}/reports`
- [ ] PostReportControllerTest

---

## Phase 5 — 마무리
- [ ] N+1 점검 (Post 목록 fetch join)
- [ ] 글로벌 예외 처리 정리 (NotFound / Forbidden / Validation)
- [ ] 전체 테스트 통과 확인
- [ ] CLAUDE.md 컨벤션 준수 셀프 체크 (Map 응답 / DTO 분리 / `new` 금지)
- [ ] 코드 리뷰 / 리팩토링
