# Production Code Review #2 — Full Project

**대상**: 전체 프로젝트 (Spring Boot 4 / Java 21 — 사용자 등록, 로그인, JWT 인증, 계정 탈퇴, 관리자 사용자 목록)
**일시**: 2026-04-03

---

## Best Case

- 레이어 간 DTO 분리 (`SignupCommand.from(request)` 등) 일관 적용
- 엔티티/DTO 모두 CLAUDE.md 컨벤션 준수 (정적 팩토리, `@NoArgsConstructor(PROTECTED)` 등)
- `Map<String, T>` 응답 없음 — `AvailableResponse` 등 전용 DTO 사용
- Refresh token rotation + 재사용 감지 올바르게 구현
- `User.withdraw()`에서 PII 익명화 및 `originalEmail` 보존 — GDPR 스타일 soft delete
- `Clock` 주입으로 시간 의존 테스트 결정론적
- `@WebMvcTest`에서 `SecurityConfig` 포함하여 401/403 시나리오 실제 필터 체인으로 테스트
- 보안 설정: STATELESS 세션, `/api/v1/admin/**`에 `ROLE_ADMIN` 제한

---

## Worst Case

- **DB 비밀번호 평문 커밋** — 레포 접근 권한이 있는 누구나 DB 직접 접속 가능
- **Admin 엔드포인트 DoS** — `?size=1000000` 요청 시 전체 users 테이블 메모리 로딩
- **탈퇴 실패** — ID >= 10,000,000 시 `VARCHAR(10)` 초과로 트랜잭션 롤백, 부분 탈퇴 상태
- **프로덕션에서 SQL 로깅 활성화** — `spring.profiles.active: dev`가 커밋되어 있어 미설정 시 모든 쿼리 로깅
- **탈퇴 후 토큰 유효** — 탈퇴 시 refresh token 무효화가 연계되지 않아 만료까지 갱신 가능

---

## Production Risks

### 🔴 Critical

#### 1. DB 비밀번호 평문 커밋

- **위치**: `src/main/resources/application.yaml`
- **문제**: `username: travelan`, `password: travelan` 이 환경변수 없이 평문 커밋. 레포 접근 권한이 있는 누구나 DB 직접 접속 가능
- **수정**: `${DB_USERNAME}`, `${DB_PASSWORD}`로 교체, 시크릿 매니저 또는 환경변수로 주입

#### 2. `User.withdraw()` nickname 오버플로우

- **위치**: `src/main/java/com/irerin/travelan/user/entity/User.java`
- **문제**: `"탈퇴" + this.id` — ID >= 10,000,000 시 `VARCHAR(10)` 초과 → `Data too long` 예외로 탈퇴 트랜잭션 전체 롤백. 사용자는 탈퇴 불가 상태
- **수정**: 컬럼을 `VARCHAR(20)`으로 확장하는 마이그레이션 추가, 또는 `"탈퇴" + (this.id % 10_000_000L)` 사용

#### 3. `AdminUserController` pagination 상한 없음 — DoS 벡터

- **위치**: `src/main/java/com/irerin/travelan/user/controller/AdminUserController.java`
- **문제**: `size` 파라미터에 `@Max` 제약 없음. `?size=2147483647` 요청 시 전체 테이블 메모리 로딩. `page=0`은 `IllegalArgumentException` → 500 반환
- **수정**:
  ```java
  @RequestParam(defaultValue = "1") @Min(1) int page,
  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ```
  클래스에 `@Validated` 추가

---

### 🟡 Warning

#### 4. Signup TOCTOU 경쟁 조건

- **위치**: `src/main/java/com/irerin/travelan/user/service/UserService.java`
- **문제**: 3개 `existsBy*` 체크 후 INSERT — 동시 요청 시 모두 통과 가능. DB 제약으로 잡히지만 에러 메시지가 `"데이터 무결성 제약 조건 위반입니다"`로 제네릭함
- **수정**: `DataIntegrityViolationException` 핸들러에서 제약 조건명 파싱하여 구체적 필드 에러 반환

#### 5. `UserHistoryRepository` 무제한 조회

- **위치**: `src/main/java/com/irerin/travelan/user/repository/UserHistoryRepository.java`
- **문제**: `findByUserOrderByCreatedAtDesc` — 페이징 없이 전체 이력 로딩
- **수정**: `Page<UserHistory>` 반환 + `Pageable` 파라미터 추가

#### 6. `spring.profiles.active: dev` 커밋

- **위치**: `src/main/resources/application.yaml`
- **문제**: 프로덕션 환경에서 프로파일 미설정 시 dev 프로파일 활성화 → `show-sql: true`로 모든 SQL 로깅
- **수정**: `application.yaml`에서 `spring.profiles.active` 제거, 환경변수로만 설정

#### 7. `logout` 수동 토큰 검증 — 필터와 중복

- **위치**: `src/main/java/com/irerin/travelan/auth/controller/AuthController.java`
- **문제**: `/api/v1/auth/**` 전체 `permitAll`로 인해 logout도 인증 미적용. 수동 토큰 파싱이 필터와 중복되고, `NumberFormatException` 시 500 반환
- **수정**: 공개 엔드포인트만 명시적 나열, logout은 `SecurityContextHolder`에서 userId 읽도록 변경

#### 8. Flyway V4, V7 마이그레이션 누락

- **위치**: `src/main/resources/db/migration/`
- **문제**: V1, V2, V3, V5, V6, V8, V9만 존재. V4, V7이 이전에 적용된 적 있으면 재배포 시 Flyway 실패
- **수정**: 누락 원인 문서화 또는 빈 placeholder 마이그레이션 파일 추가

#### 9. `revokeAllByUser`에 `@Transactional` 없음

- **위치**: `src/main/java/com/irerin/travelan/auth/repository/RefreshTokenRepository.java`
- **문제**: `@Modifying` 쿼리가 호출자의 트랜잭션 컨텍스트에 의존. 비트랜잭션 컨텍스트에서 호출 시 `TransactionRequiredException`
- **수정**: 메서드에 `@Transactional` 추가

#### 10. `interestRegions` 개별 요소 검증 없음

- **위치**: `src/main/java/com/irerin/travelan/auth/dto/SignupRequest.java`
- **문제**: 리스트 크기만 `@Size(max = 5)` 제약. 개별 요소에 빈 문자열, 100자 초과 문자열 통과
- **수정**:
  ```java
  @Size(max = 5)
  @Valid
  private List<@NotBlank @Size(max = 100) String> interestRegions;
  ```

#### 11. `UserService`가 `auth.dto.SignupResponse` 직접 반환 — 레이어 분리 위반

- **위치**: `src/main/java/com/irerin/travelan/user/service/UserService.java`
- **문제**: 서비스 레이어가 웹 레이어 DTO를 직접 반환. CLAUDE.md 컨벤션 위반
- **수정**: `user.dto.SignupResult` 도입, 서비스에서 반환 후 컨트롤러에서 `SignupResponse.from(result)` 변환

#### 12. `AdminUserController`에 `@Validated` 누락

- **위치**: `src/main/java/com/irerin/travelan/user/controller/AdminUserController.java`
- **문제**: `@Min`/`@Max` 제약 추가해도 `@Validated` 없으면 미적용
- **수정**: 클래스에 `@Validated` 추가

---

### 🔵 Improvement

#### 13. `UserInterestRegionRepository` 미사용 코드

- **위치**: `src/main/java/com/irerin/travelan/user/repository/UserInterestRegionRepository.java`
- **문제**: 어디에서도 주입되지 않는 미사용 리포지토리. 미완성 기능 스텁
- **수정**: 사용 계획이 없으면 삭제

#### 14. Spring Boot 버전 `4.0.5` 확인 필요

- **위치**: `build.gradle`
- **문제**: Spring Boot 4.x는 pre-GA 가능성. `querydsl-jpa:5.1.0` 호환성 미검증
- **수정**: 의도된 버전인지 확인. `3.4.x` 오타 가능성 검토

#### 15. `JwtProperties`에 `@Setter` — 런타임 변경 가능

- **위치**: `src/main/java/com/irerin/travelan/auth/jwt/JwtProperties.java`
- **문제**: `@Setter`로 JWT 설정이 런타임에 변경 가능. 불변이어야 함
- **수정**: `@ConstructorBinding` 또는 record 기반 `@ConfigurationProperties` 사용

#### 16. `logout` 반환 타입 `Void`이나 에러 시 JSON 반환

- **위치**: `src/main/java/com/irerin/travelan/auth/controller/AuthController.java`
- **문제**: 메서드 시그니처는 `ResponseEntity<Void>`이나 `AuthException` 발생 시 `ApiResponse<?>` JSON 반환. OpenAPI 문서와 불일치
- **수정**: 반환 타입을 `ResponseEntity<ApiResponse<Void>>`로 변경하거나 OpenAPI 어노테이션 추가

#### 17. `ErrorResponse.of(code, message)`가 `errors: []` 직렬화

- **위치**: `src/main/java/com/irerin/travelan/common/response/ErrorResponse.java`
- **문제**: 비검증 에러 시 `"errors": []` 가 항상 포함됨. 불필요한 빈 배열
- **수정**: `List.of()` → `null` 변경, `errors` 필드에 `@JsonInclude(NON_EMPTY)` 추가

#### 18. 테스트 커버리지 갭

- `logout` — 존재하지 않는 유저의 토큰으로 호출 시 경로 미테스트
- `withdraw()` — 탈퇴 시 refresh token 무효화 연계 미테스트 (탈퇴 후 `/auth/refresh` 가능)
- `AdminUserController` — `page=0` (500 유발) 및 극단적 `size` 값 미테스트

---

## 우선순위 권장 조치

| 순위 | 항목 | 심각도 |
|------|------|--------|
| 1 | DB 비밀번호를 환경변수로 교체 | Critical |
| 2 | `AdminUserController`에 `@Validated` + `@Min(1)` / `@Max(100)` 추가 | Critical |
| 3 | `User.withdraw()` nickname 안전 처리 (컬럼 확장 또는 길이 제한) | Critical |
| 4 | `spring.profiles.active: dev` 제거 | Warning |
| 5 | `SignupResponse`를 서비스 레이어 전용 DTO로 분리 | Warning |
| 6 | `interestRegions` 요소별 `@NotBlank` + `@Size(max=100)` 추가 | Warning |
| 7 | `revokeAllByUser`에 `@Transactional` 추가 | Warning |
| 8 | Flyway V4, V7 누락 원인 문서화 또는 placeholder 추가 | Warning |
| 9 | `logout` 인증 처리 개선 (permitAll 범위 축소) | Warning |
| 10 | Spring Boot 버전 확인 | Improvement |
