# 게시판 (Board) - 개발 계획

## 기술 스택
- Spring Boot, Spring Security (기존)
- JPA / Hibernate
- MySQL (또는 기존 사용 DB)
- 파일 저장: 로컬 디스크 (`./uploads/posts/{userId}/{yyyy}/{MM}/`) + `FileStorage` 인터페이스 추상화 (v2에 S3 교체)
- HTML Sanitizer: **Jsoup** (`Safelist.relaxed()` 기반)
- 테스트: JUnit 5, Spring Boot Test, MockMvc

## 개발 원칙
- TDD (RED → GREEN → REFACTOR) 엄수
- 컨트롤러 응답은 전용 DTO 사용 (Map 금지)
- 웹 DTO ↔ 서비스 Command 분리
- 엔티티: `@Builder` private + 정적 팩토리 `of(...)`

---

## Phase 1 — Region (지역 카테고리) 기반
**목표**: 운영자 사전 정의 카테고리 + 비회원 조회 가능

- Region 엔티티 / Repository / Service / Controller
- 시드 데이터 16개 (국내 8 + 인기 해외 7 + 기타 해외 1) — data.sql 또는 마이그레이션
- `GET /api/v1/regions` 비회원 허용
- SecurityConfig에 public 경로 추가

**완료 기준**: 비회원이 16개 지역 카테고리 목록을 `displayOrder` 순으로 조회할 수 있다.

---

## Phase 2 — Post CRUD (이미지 제외)
**목표**: 텍스트 기반 게시글 작성/조회/수정/삭제

- Post 엔티티 (status enum, soft delete)
- PostRepository (Region별 페이징, status 필터)
- PostService (Command 패턴)
- PostController
  - `GET /api/v1/regions/{code}/posts` (비회원 허용)
  - `GET /api/v1/posts/{id}` (비회원 허용, viewCount 증가)
  - `POST /api/v1/regions/{code}/posts` (인증 필수)
  - `PUT /api/v1/posts/{id}` (작성자만)
  - `DELETE /api/v1/posts/{id}` (작성자/운영자, soft delete)
- 권한 검사 로직 (작성자 == 현재 사용자 OR 운영자)
- 본문 HTML Sanitize 처리 (Jsoup `Safelist.relaxed()` 기반 유틸 + create/update 양쪽 적용)
- 탈퇴 사용자 작성 글 유지 검증 (통합 테스트)

**완료 기준**: 회원이 글을 쓰고 비회원이 읽을 수 있다. 작성자만 수정/삭제 가능.

---

## Phase 3 — 이미지 첨부
**목표**: 게시글에 이미지 업로드/표시

- `FileStorage` 인터페이스 + `LocalFileStorage` 구현
  - 저장 경로 규약: `./uploads/posts/{userId}/{yyyy}/{MM}/{uuid}.{ext}`
- PostImage 엔티티
- ImageController: `POST /api/v1/posts/images` (다중 업로드)
- 파일 검증: 확장자(jpg/png/webp), 크기(5MB), 개수(10장)
- 게시글 작성/수정 시 `imageIds`로 연결
- 정적 리소스 서빙 설정
- 게시글 삭제 시 이미지 처리 (soft delete만, 파일은 유지)

**완료 기준**: 이미지를 첨부한 글을 작성하고 비회원이 이미지와 함께 열람할 수 있다.

---

## Phase 4 — 신고 기능
**목표**: 최소 모더레이션 인프라

- PostReport 엔티티
- ReportRepository (중복 신고 방지: `existsByPostAndReporter`)
- ReportService / Controller
- `POST /api/v1/posts/{id}/reports` (인증 필수, 자기글 신고 금지)

**완료 기준**: 회원이 부적절한 글을 신고할 수 있고, 동일 글 중복 신고가 차단된다.

---

## Phase 5 — 마무리 / 리팩토링
- 통합 테스트 보강 (비회원 조회, 권한 분리, 탈퇴 사용자 케이스)
- 에러 응답 포맷 일관화
- API 문서화 (필요 시)
- 성능 점검 (N+1 확인: Post ↔ PostImage, Post ↔ Author)

---

## 리스크 / 주의사항
- **N+1 쿼리**: 게시글 목록에서 작성자/이미지 fetch join 필요
- **soft delete 누수**: 모든 조회 쿼리에서 `status = PUBLISHED` 필터 일관 적용 (글로벌 필터 검토)
- **파일 업로드 보안**: 확장자 화이트리스트 + Content-Type 검증 + 파일명 sanitize
- **권한 우회**: 수정/삭제 시 반드시 작성자 검증을 서비스 레이어에서 수행
- **XSS 방지**: 본문은 저장 시점에 Jsoup으로 sanitize 강제. 클라이언트 입력을 그대로 저장 금지.
