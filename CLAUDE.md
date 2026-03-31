# CLAUDE.md

## 개발 방식

### TDD (Test-Driven Development)
기능 구현 시 반드시 RED → GREEN → REFACTOR 순서를 따를 것.

1. **RED** — 실패하는 테스트를 먼저 작성
2. **GREEN** — 테스트를 통과시키는 최소한의 구현 작성
3. **REFACTOR** — 동작을 유지하면서 코드 품질 개선

---

## 코드 컨벤션

### API 응답 타입
- 컨트롤러 응답으로 `Map<String, T>`를 사용하지 말 것
- 단순한 값 하나라도 전용 DTO 클래스로 감쌀 것
- 예: `{ "available": true }` → `AvailableResponse` 클래스 사용
- **예외**: `ApiResponse.Builder`의 `.meta(String key, Object value)`는 허용. 응답 봉투(envelope) 내부의 추가 집계 메타데이터(totalCount, ratingAverage 등)를 키-값으로 추가할 때만 사용할 것
  ```java
  ApiResponse.<List<ReviewResponse>>builder()
      .data(page.getContent())
      .page(PageMeta.from(page, pageNumber))
      .search(search)
      .meta("totalCount", service.count(search))
      .meta("ratingAverage", service.getRatingAverage(search))
      .build();
  ```

### 레이어 간 DTO 분리
- 컨트롤러(웹 레이어) DTO를 서비스 레이어에 직접 전달하지 말 것
- 컨트롤러에서 서비스 레이어 전용 Command/DTO로 변환한 뒤 전달할 것
- 웹 레이어 DTO: `auth.dto` (예: `SignupRequest`)
- 서비스 레이어 DTO: `{domain}.dto` (예: `user.dto.SignupCommand`)
- 변환은 Command의 정적 팩토리 메서드 `from(request)`로 처리

### 객체 생성 방식
- `new` 연산자를 직접 사용하지 말 것
- 엔티티: `@NoArgsConstructor(access = PROTECTED)` + `@Builder` private 생성자 + 정적 팩토리 메서드 `of(...)` 사용
- DTO / 응답 객체: `@AllArgsConstructor(access = PRIVATE)` + 정적 팩토리 메서드 `from(...)` 또는 `of(...)` 사용
- 예외 (`throw new ...`)·서드파티 클래스·컬렉션 필드 초기화(`new ArrayList<>()`)는 예외적으로 허용
