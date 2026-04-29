# 게시판 (Board) - Tasks

## Phase 0 — 사전 결정 (완료)
- [x] 본문 포맷 → **Sanitized HTML** (Jsoup `Safelist.relaxed()`)
- [x] 초기 Region 시드 → **16개**: 국내(서울/경기/인천/강원/충청/전라/경상/제주) + 인기 해외(일본/중국/베트남/태국/필리핀/대만/미국) + 기타 해외
- [x] 이미지 저장 경로 → **`./uploads/posts/{userId}/{yyyy}/{MM}/{uuid}.{ext}`**

---

## Phase 1 — Region
- [x] Region 엔티티 작성 (`@Builder` private + `of(...)`)
- [x] RegionRepository
- [x] RegionRepository 테스트 (RED → GREEN)
- [x] RegionService — `findAllActive()`
- [x] RegionService 테스트
- [x] RegionResponse DTO
- [x] RegionController — `GET /api/v1/regions`
- [x] RegionControllerTest (MockMvc, 비회원 200)
- [x] SecurityConfig: `/api/v1/regions/**` GET 허용
- [x] 시드 데이터 추가 (data.sql 또는 마이그레이션) — 16개 Region (국내 8 → 인기 해외 7 → 기타 해외 1 순으로 displayOrder 부여)
- [x] 통합 테스트: 비회원 카테고리 조회 (16개 정렬 검증)

---

## Phase 2 — Post CRUD
### 엔티티 / 리포지토리
- [x] PostStatus enum (PUBLISHED, DELETED, BLOCKED)
- [x] Post 엔티티 (Region, User FK, soft delete)
- [x] Post 엔티티 테스트 (`of`, `update`, `delete`, 권한 체크 메서드)
- [x] PostRepository — `findByRegionAndStatus(...)`, `findByIdAndStatusNot(...)`
- [x] PostRepository 테스트

### HTML Sanitizer
- [x] HtmlSanitizer 유틸 작성 (Jsoup `Safelist.relaxed()` 기반)
- [x] HtmlSanitizer 테스트 (`<script>`, `onclick=`, `<iframe>` 제거 검증 / 정상 인라인 서식 유지 검증)

### 서비스
- [x] CreatePostCommand / UpdatePostCommand
- [x] PostService.create / get / list / update / delete (create·update에서 본문 sanitize 적용)
- [x] PostService 테스트 (작성자 권한, 운영자 권한, 탈퇴 사용자 케이스, sanitize 적용 케이스)

### 컨트롤러
- [x] CreatePostRequest / UpdatePostRequest (웹 DTO) + Command 변환
- [x] PostSummaryResponse / PostDetailResponse
- [x] PostController — 5개 엔드포인트
- [x] PostControllerTest (각 권한 시나리오)
- [x] SecurityConfig: 조회 GET 비회원 허용, 나머지 인증

### 통합
- [ ] 통합 테스트: 비회원 조회 → 회원 작성 → 작성자 수정 → 운영자 삭제
- [ ] 통합 테스트: 탈퇴 사용자 작성 글이 목록/상세에서 정상 노출되는지

---

## Phase 3 — 이미지 첨부
- [x] FileStorage 인터페이스 (`store(userId, file)`, `delete(path)`, `toUrl(path)`)
- [x] LocalFileStorage 구현 — 경로 규약 `./uploads/posts/{userId}/{yyyy}/{MM}/{uuid}.{ext}`
- [x] LocalFileStorage 테스트 (임시 디렉터리, 경로 규약 검증)
- [x] PostImage 엔티티
- [x] PostImageRepository
- [x] ImageUploadService — 검증 (확장자 화이트리스트, Content-Type, 크기, 개수)
- [x] ImageUploadService 테스트 (실패 케이스 포함: 잘못된 확장자, Content-Type 불일치, 초과 크기)
- [x] PostImageResponse DTO
- [x] ImageController — `POST /api/v1/posts/images`
- [x] ImageControllerTest (multipart)
- [x] Post 작성/수정 시 `imageIds` 연결 로직
- [x] 정적 리소스 서빙 설정 (WebMvcConfigurer)
- [x] V15 마이그레이션 (post_images 테이블)
- [x] GlobalExceptionHandler — InvalidFileException → 400
- [x] SecurityConfig — /uploads/** GET permitAll
- [x] application.yaml — multipart 설정, upload root 설정
- [ ] 통합 테스트: 이미지 업로드 → 게시글 작성 → 비회원 조회 시 URL 포함

---

## Phase 4 — 신고
- [x] ReportReason enum
- [x] PostReport 엔티티
- [x] PostReportRepository — `existsByPostIdAndReporterId`
- [x] PostReportRepository 테스트
- [x] ReportPostCommand
- [x] PostReportService — 자기글 신고 금지, 중복 금지
- [x] PostReportService 테스트
- [x] CreateReportRequest / ReportResponse
- [x] PostReportController — `POST /api/v1/posts/{id}/reports`
- [x] PostReportControllerTest

---

## Phase 5 — 자동 블라인드
- [ ] PostReport 누적 횟수 임계값 설정 (application.yaml)
- [ ] 임계값 도달 시 Post 상태를 BLOCKED로 자동 변경
- [ ] 블라인드 해제(복원) 로직 (관리자 전용)
- [ ] 오남용 방지 대책 검토 (조직적 신고 대응)
- [ ] 자동 블라인드 테스트 (임계값 도달, 복원, 엣지 케이스)

---

## Phase 6 — 마무리
- [ ] N+1 점검 (Post 목록 fetch join)
- [ ] 글로벌 예외 처리 정리 (NotFound / Forbidden / Validation)
- [ ] 전체 테스트 통과 확인
- [ ] CLAUDE.md 컨벤션 준수 셀프 체크 (Map 응답 / DTO 분리 / `new` 금지)
- [ ] 코드 리뷰 / 리팩토링

---

## Phase 7 — viewCount 정책 후속 (#9 후속)

`PostService.get()` 의 view count 증가는 `@Modifying UPDATE`로 RMW race를 제거했지만, 다음 정책 결정과 추가 작업이 남아있다.

- [ ] **자기 게시글 view 제외 정책 결정** — 작성자가 자기 글을 열어도 viewCount가 증가하는 현재 동작이 맞는지 결정. 제외하기로 하면 `incrementViewCount`에 viewer 식별자 조건 추가 또는 service 레이어에서 분기
- [ ] **세션/IP 기반 view 중복 제거(dedup)** — 동일 사용자가 새로고침 100번 시 100 증가하는 현 동작 보완. Redis TTL 캐시 또는 쿠키 기반으로 단위 시간 내 중복 카운트 차단 검토
- [ ] **viewCount 무증가 조회용 메서드 분리** — 관리자 도구/내부 API 등에서 view 카운트를 올리지 않고 게시글을 열어야 하는 경우를 위해 `getWithoutIncrement(...)` 또는 플래그 방식 검토
- [ ] **고트래픽 게시글 대비 비동기/배치 카운터** — 인기 게시글에서 매 조회 UPDATE가 master DB 부하로 누적되는 패턴이 보이면 Redis INCR + 배치 flush 또는 큐 기반 집계로 전환 검토 (지금은 과한 도입이라 미뤄둠)

---

## Phase 8 — PostHistory 후속 (#12 후속)

`PostService.create()`에서 CREATED 액션 스냅샷을 남기도록 변경했지만, 다음
후속 작업이 남아있다.

- [ ] **기존 게시글 backfill 마이그레이션** — 변경 이전에 작성된 posts에는
  CREATED PostHistory가 없다. `INSERT INTO post_history(post_id, action,
  title, content, editor_id, created_at) SELECT id, 'CREATED', title,
  content, author_id, created_at FROM posts WHERE NOT EXISTS (SELECT 1
  FROM post_history h WHERE h.post_id = posts.id AND h.action = 'CREATED')`
  같은 형태로 일괄 보정. 운영 데이터 규모 작을 때 한 번만 실행
- [ ] **블라인드 액션 도입** — Phase 5 자동 블라인드 추가 시 BLOCKED /
  UNBLOCKED 액션을 PostHistoryAction에 추가하고 상태 전환 시 스냅샷 저장
- [ ] **신고는 PostHistory가 아닌 PostReport로 분리 유지** — 진실의 원천
  분리 정책 명시. PostHistory에 REPORTED 같은 액션 추가하지 않는다는 결정
  문서화
