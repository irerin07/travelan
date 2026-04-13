# Phase 3 이미지 첨부 — 코드 리뷰 결과

## Critical (배포 차단)

### C1. IDOR — 다른 사용자의 이미지를 자기 게시글에 첨부 가능
- **위치**: `PostService.attachImages`
- **문제**: `post_images`에 `uploader_id` 컬럼이 없어 소유권 검증 불가. 아무 유저가 다른 유저의 이미지 ID를 자기 게시글에 첨부 가능
- **수정**: `post_images`에 `uploader_id BIGINT NOT NULL` FK 추가, `ImageUploadService.upload`에서 저장, attach 쿼리에 `uploaderId` 필터 추가

### C2. 매직바이트 미검증 — Content-Type 위조 가능
- **위치**: `ImageUploadService.validate`
- **문제**: 확장자/Content-Type은 클라이언트가 조작 가능. SVG+JS를 jpg로 위장 시 Stored XSS 가능
- **수정**: 파일의 첫 4~12바이트를 읽어 JPEG(`FF D8 FF`), PNG(`89 50 4E 47`), WebP(`52 49 46 46...57 45 42 50`) 매직바이트 검증 추가

### C3. 트랜잭션 롤백 시 물리 파일 잔존
- **위치**: `ImageUploadService.upload`
- **문제**: 3번째 파일 검증 실패 시 DB는 롤백되지만 1~2번 파일은 디스크에 남음
- **수정**: 모든 파일 검증을 먼저 수행한 후, 파일 저장+DB 저장 루프 실행

---

## Warning (다음 스프린트 수정)

### W1. 게시글 수정 시 고아 파일 누적
- **문제**: `detachImages`에서 FK만 null 처리하고 파일/DB행 삭제 안 함
- **수정**: detach 후 `fileStorage.delete(path)` + `postImageRepository.deleteAll(existing)` 호출

### W2. imageIds 불일치 시 무시
- **문제**: `imageIds` 중 일부만 유효해도 에러 없이 진행, 클라이언트에 알림 없음
- **수정**: `images.size() != imageIds.size()`이면 `NotFoundException` 또는 400 에러 반환

### W7. Post.@Builder access 누락
- **문제**: `Post.java`의 `@Builder`에 `access = PRIVATE` 누락 (CLAUDE.md 위반)
- **수정**: `@Builder(access = AccessLevel.PRIVATE)` 적용

---

## Improvement (기술 부채)

### I1. post_images.url에 공개 URL 저장
- **문제**: `/uploads/posts/1/...` 전체 URL 저장 → 스토리지 변경 시 전체 마이그레이션 필요
- **권장**: 상대 경로(`posts/1/...`)만 저장, 조회 시 `fileStorage.toUrl(path)`로 변환

### I3. PostServiceTest 이미지 테스트 부재
- **문제**: `PostServiceTest`에 이미지 attach/detach 경로 테스트 없음
- **권장**: create/update에 imageIds 포함 테스트 추가

### I5. ImageController 응답 의미 불일치
- **문제**: `@ResponseStatus(CREATED)` (201)인데 `ApiResponse.ok()` 사용
- **권장**: `ApiResponse.created()` 팩토리 추가 또는 네이밍 정리
