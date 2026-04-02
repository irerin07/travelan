# Travelan 코드 리뷰 & 개선 사항

---

## 1. UserService.signup() — 탈퇴 회원 재가입 로직의 Race Condition

### 현재 코드
```java
// UserService.java
userRepository.findByEmail(request.getEmail()).ifPresent(userRepository::delete);
userRepository.findByPhone(request.getPhone()).ifPresent(userRepository::delete);
userRepository.findByNicknameIgnoreCase(request.getNickname()).ifPresent(userRepository::delete);
```

### 문제
1. **다른 ACTIVE 유저를 삭제할 수 있다.** `existsByEmailAndStatusNot(WITHDRAWN)`으로 중복 검사를 통과한 뒤, `findByEmail()`은 status 조건 없이 조회하므로 ACTIVE/SUSPENDED 유저도 삭제 대상이 된다.
2. **동시 요청 시 Race Condition.** 두 사용자가 동시에 동일 이메일로 가입하면, 둘 다 중복 체크를 통과하고 한쪽이 다른 쪽의 데이터를 삭제할 수 있다.
3. **3건의 개별 조회+삭제.** 탈퇴 유저 정리를 위해 6번의 DB 호출이 발생한다.

### Best Practice
- WITHDRAWN 레코드를 **삭제하지 않고 보존**하여 감사 추적(audit trail)을 지원한다.
- **탈퇴 시점에 고유 필드를 익명화**하여 unique 제약 충돌을 해소한다.
- `withdrawn_at`, `original_email` 컬럼으로 탈퇴 이력을 추적한다.
- 가입 시에는 중복 체크만 수행하고, 레코드 삭제를 하지 않는다.

### 개선 방법

**1) User 엔티티에 탈퇴 관련 필드 및 메서드 추가**
```java
// User.java
@Column
private String originalEmail;

@Column
private LocalDateTime withdrawnAt;

public void withdraw() {
    this.originalEmail = this.email;
    this.email = "withdrawn_" + this.id + "@deleted";
    this.phone = "del_" + this.id;
    this.nickname = "탈퇴" + this.id;
    this.status = UserStatus.WITHDRAWN;
    this.withdrawnAt = LocalDateTime.now();
}
```

**2) signup()에서 레코드 삭제 로직 제거**
```java
// UserService.java — 삭제 3줄 완전 제거
@Transactional
public SignupResponse signup(SignupCommand request) {
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new DuplicateException("이미 사용 중인 이메일입니다");
    }
    if (userRepository.existsByPhone(request.getPhone())) {
        throw new DuplicateException("이미 사용 중인 휴대폰 번호입니다");
    }
    if (userRepository.existsByNicknameIgnoreCase(request.getNickname())) {
        throw new DuplicateException("이미 사용 중인 닉네임입니다");
    }
    // findByEmail().ifPresent(delete) 없음 — WITHDRAWN 유저는 익명화 완료 상태
    // ...
}
```

**3) Flyway 마이그레이션**
```sql
-- V8__add_withdrawal_columns_to_users.sql
ALTER TABLE users
    ADD COLUMN original_email VARCHAR(255) NULL,
    ADD COLUMN withdrawn_at DATETIME NULL;
```

### 관련 파일
- `src/main/java/com/irerin/travelan/user/entity/User.java`
- `src/main/java/com/irerin/travelan/user/service/UserService.java`
- `src/main/java/com/irerin/travelan/user/repository/UserRepository.java`
- `src/main/resources/db/migration/V8__add_withdrawal_columns_to_users.sql`

---

## 2. DataIntegrityViolationException 미처리

### 현재 상태
`GlobalExceptionHandler`에 `DataIntegrityViolationException` 핸들러가 없다.
DB unique 제약(email, phone, nickname)에 의한 중복 삽입이 발생하면 500 Internal Server Error로 응답한다.

### 문제
- Race condition이나 예기치 않은 동시 요청에서 DB 제약 위반이 발생할 수 있다.
- 사용자에게 `500 Internal Server Error` 대신 의미 있는 `409 Conflict`를 반환해야 한다.

### Best Practice
어플리케이션 레벨 중복 체크(`existsBy...`)는 **성능 최적화**이고, DB unique 제약은 **최종 방어선**이다. 둘 다 있어야 한다.

### 개선 방법
```java
// GlobalExceptionHandler.java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ApiResponse<?>> handleDataIntegrity(DataIntegrityViolationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error(ErrorResponse.of("DUPLICATE", "이미 존재하는 데이터입니다")));
}
```

### 관련 파일
- `src/main/java/com/irerin/travelan/common/exception/GlobalExceptionHandler.java`

---

## 3. JwtProvider — SecretKey 매 요청마다 재생성

### 현재 코드
```java
// JwtProvider.java
private SecretKey getSecretKey() {
    return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
}
```

### 문제
`getSecretKey()`가 호출될 때마다 `secret.getBytes()` → `Keys.hmacShaKeyFor()` 연산이 반복된다.
토큰 검증(`isValid`) + 파싱(`getUserId`, `getRole`)이 매 HTTP 요청마다 실행되므로 불필요한 객체 생성이 누적된다.

### Best Practice
불변 값 기반의 키 생성은 초기화 시 한 번만 수행하고 캐싱한다.

### 개선 방법
```java
@Component
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(
            jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    // getSecretKey() 메서드 제거, 필드 직접 사용
}
```

### 관련 파일
- `src/main/java/com/irerin/travelan/auth/jwt/JwtProvider.java`

---

## 4. SignupCommand — 컨벤션 불일치

### 현재 코드
```java
// SignupCommand.java
@Getter
@RequiredArgsConstructor
public class SignupCommand {
    private final String email;
    // ...

    public static SignupCommand from(SignupRequest request) {
        return new SignupCommand(...);
    }
}
```

### 문제
CLAUDE.md 컨벤션: `@AllArgsConstructor(access = PRIVATE)` + 정적 팩토리.
`SignupCommand`만 `@RequiredArgsConstructor`(public 생성자)를 사용하고 있다.
다른 DTO(`LoginCommand`, `LoginTokens`, `LoginResponse` 등)는 모두 컨벤션을 따르고 있다.

### 개선 방법
```java
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SignupCommand {
    // ...
}
```

### 관련 파일
- `src/main/java/com/irerin/travelan/user/dto/SignupCommand.java`

---

## 5. RefreshToken — 로그인 시 기존 토큰 정리 없음

### 현재 코드
```java
// AuthService.java - login()
refreshTokenRepository.save(RefreshToken.of(
    user, refreshToken,
    LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpiry())
));
```

### 문제
로그인할 때마다 새 RefreshToken을 추가만 하고, 기존 토큰을 정리하지 않는다.
한 유저가 100번 로그인하면 100개의 RefreshToken 행이 DB에 쌓인다.

### Best Practice
- **단일 세션 정책**: 로그인 시 기존 토큰을 전부 revoke하고 새 토큰만 유지
- **다중 세션 정책**: 허용하되 만료된 토큰을 주기적으로 정리하는 배치 필요

### 개선 방법 (단일 세션)
```java
@Transactional
public LoginTokens login(LoginCommand command) {
    // ... 인증 로직 ...

    // 기존 토큰 revoke
    refreshTokenRepository.revokeAllByUser(user);

    // 새 토큰 저장
    refreshTokenRepository.save(RefreshToken.of(...));

    return LoginTokens.of(...);
}
```

```java
// RefreshTokenRepository.java
@Modifying
@Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
void revokeAllByUser(@Param("user") User user);
```

### 관련 파일
- `src/main/java/com/irerin/travelan/auth/service/AuthService.java`
- `src/main/java/com/irerin/travelan/auth/repository/RefreshTokenRepository.java`

---

## 6. SecurityConfig — JSON 응답 하드코딩

### 현재 코드
```java
private static final String UNAUTHORIZED_BODY =
    "{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\",\"errors\":[]}}";

private static final String FORBIDDEN_BODY =
    "{\"success\":false,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"접근 권한이 없습니다\",\"errors\":[]}}";
```

### 문제
- `ApiResponse` + `ErrorResponse`로 동일 구조를 이미 구현해 두었는데, SecurityConfig에서는 수동 JSON 문자열을 사용한다.
- ApiResponse 구조가 변경되면 SecurityConfig의 JSON도 수동으로 맞춰야 한다.
- ObjectMapper가 아닌 수동 문자열이므로 JSON 형식 오류를 컴파일 타임에 잡을 수 없다.

### Best Practice
Spring Security의 `AuthenticationEntryPoint`와 `AccessDeniedHandler`에서도 `ObjectMapper`를 사용하여 일관된 응답 구조를 생성한다.

### 개선 방법
```java
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtProvider jwtProvider) throws Exception {
        http
            // ...
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    ApiResponse<?> body = ApiResponse.error(
                        ErrorResponse.of("UNAUTHORIZED", "인증이 필요합니다"));
                    response.getWriter().write(objectMapper.writeValueAsString(body));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    ApiResponse<?> body = ApiResponse.error(
                        ErrorResponse.of("FORBIDDEN", "접근 권한이 없습니다"));
                    response.getWriter().write(objectMapper.writeValueAsString(body));
                })
            );
        return http.build();
    }
}
```

### 관련 파일
- `src/main/java/com/irerin/travelan/common/config/SecurityConfig.java`

---

## 7. RefreshToken.isExpired() — 테스트하기 어려운 시간 의존 코드

### 현재 코드
```java
// RefreshToken.java
public boolean isExpired() {
    return LocalDateTime.now().isAfter(this.expiresAt);
}
```

### 문제
`LocalDateTime.now()`를 직접 호출하므로 단위 테스트에서 시간을 제어할 수 없다.
만료 검증 테스트를 작성하려면 실제로 시간이 지나길 기다리거나, 과거 `expiresAt`을 설정해야 한다.

### Best Practice
`java.time.Clock`을 주입받아 시간 의존성을 외부에서 제어한다.

### 개선 방법
```java
// JpaConfig.java
@Bean
public Clock clock() {
    return Clock.systemDefaultZone();
}
```

```java
// RefreshToken.java
public boolean isExpired(Clock clock) {
    return LocalDateTime.now(clock).isAfter(this.expiresAt);
}

public boolean isUsable(Clock clock) {
    return !revoked && !isExpired(clock);
}
```

또는 `AuthService`에서 `Clock`을 주입받고, 엔티티는 시간을 파라미터로 받는 방식:
```java
// AuthService.java
public boolean isUsable(LocalDateTime now) {
    return !revoked && now.isBefore(expiresAt);
}
```

### 관련 파일
- `src/main/java/com/irerin/travelan/auth/entity/RefreshToken.java`
- `src/main/java/com/irerin/travelan/common/config/JpaConfig.java`

---

## 8. check-* API — @RequestParam 유효성 검증 누락

### 현재 코드
```java
@GetMapping("/check-email")
public ResponseEntity<ApiResponse<AvailableResponse>> checkEmail(@RequestParam String email) {
    return ResponseEntity.ok(ApiResponse.ok(AvailableResponse.of(userService.isEmailAvailable(email))));
}
```

### 문제
- `email`, `phone`, `nickname`에 대한 유효성 검증이 없다.
- 빈 문자열이나 유효하지 않은 형식이 그대로 DB 쿼리에 전달된다.
- `GET /api/v1/auth/check-email?email=` → DB에 `WHERE email = ''` 쿼리 실행.

### Best Practice
시스템 경계(사용자 입력)에서 반드시 유효성 검증을 수행한다.

### 개선 방법
컨트롤러 클래스에 `@Validated` 추가 + `@RequestParam`에 Bean Validation 적용:

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated  // 추가
public class AuthController {

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<AvailableResponse>> checkEmail(
        @RequestParam @NotBlank @Email String email) {
        // ...
    }

    @GetMapping("/check-phone")
    public ResponseEntity<ApiResponse<AvailableResponse>> checkPhone(
        @RequestParam @NotBlank @Pattern(regexp = "^010\\d{8}$") String phone) {
        // ...
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<AvailableResponse>> checkNickname(
        @RequestParam @NotBlank @Size(min = 2, max = 10) String nickname) {
        // ...
    }
}
```

`ConstraintViolationException` 핸들러 추가:
```java
// GlobalExceptionHandler.java
@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {
    List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
        .map(v -> ErrorResponse.FieldError.of(
            extractFieldName(v.getPropertyPath()),
            v.getMessage()))
        .toList();
    return ResponseEntity.badRequest()
        .body(ApiResponse.error(ErrorResponse.of("VALIDATION_ERROR", "입력값이 올바르지 않습니다", fieldErrors)));
}
```

### 관련 파일
- `src/main/java/com/irerin/travelan/auth/controller/AuthController.java`
- `src/main/java/com/irerin/travelan/common/exception/GlobalExceptionHandler.java`

---

## 9. User.interestRegions — 중복 지역 방지 없음

### 현재 코드
```java
// User.java
public void addInterestRegion(String region) {
    this.interestRegions.add(UserInterestRegion.of(this, region));
}
```

### 문제
동일 지역을 여러 번 추가해도 차단하지 않는다.
`["유럽", "유럽", "유럽"]`으로 요청하면 3건이 저장된다.

### Best Practice
비즈니스 규칙(최대 5개)과 함께 중복도 검증해야 한다.

### 개선 방법
```java
// UserService.java - signup()
List<String> regions = request.getInterestRegions();
if (regions != null) {
    regions.stream().distinct().forEach(user::addInterestRegion);
}
```

또는 엔티티에서 방어:
```java
// User.java
public void addInterestRegion(String region) {
    boolean exists = this.interestRegions.stream()
        .anyMatch(r -> r.getRegion().equals(region));
    if (!exists) {
        this.interestRegions.add(UserInterestRegion.of(this, region));
    }
}
```

### 관련 파일
- `src/main/java/com/irerin/travelan/user/entity/User.java`
- `src/main/java/com/irerin/travelan/user/service/UserService.java`

---

## 10. AuthController.login() — Refresh Token Cookie maxAge 하드코딩

### 현재 코드
```java
ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
    .maxAge(Duration.ofDays(30))  // 하드코딩
    // ...
```

### 문제
Refresh Token의 만료 시간은 `application.yaml`에서 `refresh-token-expiry: 2592000`(30일)로 관리하고 있지만, Cookie의 `maxAge`는 별도로 30일을 하드코딩하고 있다. 설정값을 변경해도 Cookie에는 반영되지 않는다.

### 개선 방법
```java
ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
    .maxAge(Duration.ofSeconds(jwtProperties.getRefreshTokenExpiry()))
    // ...
```

`AuthController`에 `JwtProperties` 의존성 추가 필요.

### 관련 파일
- `src/main/java/com/irerin/travelan/auth/controller/AuthController.java`

---

## 11. application.yaml — show-sql 개발 전용 설정이 기본값에 포함

### 현재 상태
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

### 문제
`show-sql: true`와 `format_sql: true`가 기본 설정에 있으므로 프로덕션에서도 SQL이 콘솔에 출력된다.
SQL 로그는 성능에 영향을 주고, 민감한 쿼리 정보가 노출될 수 있다.

### Best Practice
프로필(`application-dev.yaml`, `application-prod.yaml`)을 분리하여 환경별 설정을 관리한다.

### 개선 방법
```yaml
# application.yaml (공통)
spring:
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false

# application-dev.yaml (개발 전용)
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

### 관련 파일
- `src/main/resources/application.yaml`

---

## 12. ApiResponse.ofPage() — Builder 도입 이후 불필요한 정적 메서드

### 현재 코드
```java
public static <T> ApiResponse<List<T>> ofPage(Page<T> pageResult, int pageNumber) {
    return new ApiResponse<>(true, pageResult.getContent(), PageMeta.from(pageResult, pageNumber), null, null, null);
}
```

### 문제
Builder의 `data(Page<E>)` 오버로드가 동일한 기능을 제공하며, 페이지 번호도 자동 변환한다.
`ofPage()`는 사용처가 없는 레거시 메서드다.

### 개선 방법
`ofPage()` 메서드를 삭제한다. 현재 사용처가 있는지 검색 후 없으면 제거.

### 관련 파일
- `src/main/java/com/irerin/travelan/common/response/ApiResponse.java`

---

## 13. 테스트 — 누락된 테스트 커버리지

### 현재 상태
- `UserServiceTest`: signup 성공, 중복 예외 3건, findUsers 4건 = 8개
- `AuthServiceTest`: login 성공/실패 = 5개
- `AuthControllerTest`: signup 9개 + login 3개 = 12개
- `AdminUserControllerTest`: 5개
- `JwtProviderTest`: 7개
- `JwtAuthenticationFilterTest`: 3개
- `ApiResponseTest`: 10개

### 누락된 영역

| 누락 영역 | 설명 |
|-----------|------|
| **UserService.signup() — 관심 지역 저장** | `interestRegions`가 null일 때와 값이 있을 때의 분기 미검증 |
| **UserService — check API 메서드** | `isEmailAvailable`, `isPhoneAvailable`, `isNicknameAvailable` 단위 테스트 없음 |
| **AuthService — RefreshToken 저장 검증** | login 성공 시 RefreshToken이 실제로 save되는지 verify 없음 |
| **AuthController — login Set-Cookie 속성** | `Secure`, `SameSite`, `Path` 속성까지 상세 검증 없음 |
| **GlobalExceptionHandler** | 500 에러 핸들러 테스트 없음 |

### 관련 파일
- `src/test/java/com/irerin/travelan/` 하위 전체

---

## 우선순위 요약

| 우선순위 | 항목 | 심각도 | 이유 |
|---------|------|--------|------|
| **P0** | #1 탈퇴 회원 재가입 — ACTIVE 유저 삭제 가능 | 치명적 | 데이터 손실 위험 |
| **P0** | #2 DataIntegrityViolationException 미처리 | 높음 | 사용자에게 500 에러 노출 |
| **P1** | #5 RefreshToken 무한 누적 | 높음 | 데이터 무한 증가, 성능 저하 |
| **P1** | #8 check-* 유효성 검증 누락 | 중간 | 불필요한 DB 쿼리 실행 |
| **P1** | #9 관심 지역 중복 허용 | 중간 | 데이터 무결성 위반 |
| **P2** | #3 SecretKey 매 요청 재생성 | 낮음 | 불필요한 객체 생성 |
| **P2** | #4 SignupCommand 컨벤션 불일치 | 낮음 | 코드 일관성 |
| **P2** | #6 SecurityConfig JSON 하드코딩 | 낮음 | 유지보수성 |
| **P2** | #7 시간 의존 코드 | 낮음 | 테스트 용이성 |
| **P2** | #10 Cookie maxAge 하드코딩 | 낮음 | 설정 불일치 위험 |
| **P3** | #11 show-sql 프로필 미분리 | 낮음 | 프로덕션 배포 시 주의 |
| **P3** | #12 ofPage 레거시 메서드 | 매우 낮음 | 코드 정리 |
| **P3** | #13 테스트 커버리지 | 낮음 | 품질 보증 |
