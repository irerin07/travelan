# Travelan 리팩토링 태스크

> refactor.md 기반. 우선순위(P0→P3) 순서. TDD: RED → GREEN → REFACTOR.

---

## Step 1 — P0: 탈퇴 회원 재가입 로직 수정 (refactor.md #1)

> 방침: WITHDRAWN 레코드를 삭제하지 않고 보존한다.
> 탈퇴 시 고유 필드(email, phone, nickname)를 익명화하여 unique 제약 충돌을 해소한다.
> `withdrawn_at`, `original_email` 컬럼을 추가하여 감사 추적(audit trail)을 지원한다.

### 1-1. DB 마이그레이션
- [x] `V8__add_withdrawal_columns_to_users.sql` 작성
  - `ADD COLUMN original_email VARCHAR(255) NULL`
  - `ADD COLUMN withdrawn_at DATETIME NULL`

### 1-2. RED: 테스트 작성
- [x] `UserTest` — `withdraw()` 호출 시 status=WITHDRAWN, email/phone/nickname 익명화, originalEmail 보존, withdrawnAt 설정 검증
- [x] `UserServiceTest` — WITHDRAWN 유저(익명화 완료) 존재 시 동일 이메일로 재가입 성공 테스트

### 1-3. GREEN: 구현
- [x] `User` 엔티티에 `originalEmail` (nullable), `withdrawnAt` (nullable) 필드 추가
- [x] `User.withdraw()` 메서드 작성
  - `this.originalEmail = this.email`
  - `this.email = "withdrawn_" + this.id + "@deleted"`
  - `this.phone = "del_" + this.id`
  - `this.nickname = "탈퇴" + this.id`
  - `this.status = UserStatus.WITHDRAWN`
  - `this.withdrawnAt = LocalDateTime.now()` (추후 Step 9에서 Clock 적용)
- [x] `UserService.signup()`에서 `findByEmail/Phone/Nickname().ifPresent(delete)` 3줄 삭제
- [x] `UserRepository`에서 `findByPhone()`, `findByNicknameIgnoreCase()` 제거 (signup 삭제 용도로만 사용되었으므로)

### 1-4. REFACTOR: 중복 체크 단순화
- [x] `existsByEmailAndStatusNot(WITHDRAWN)` → `existsByEmail()` 변경 검토 (WITHDRAWN 유저는 익명화된 이메일이므로 충돌 없음)
- [x] 단, SUSPENDED 유저도 중복 차단해야 하므로 `existsByEmail()`으로 단순화 가능
- [x] `isEmailAvailable`, `isPhoneAvailable`, `isNicknameAvailable`도 동일하게 단순화

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN
- [x] `UserService.signup()`에 레코드 삭제 로직이 없음
- [x] `withdraw()` 호출 시 원본 이메일이 `originalEmail`에 보존됨
- [x] `withdraw()` 호출 시 email/phone/nickname이 익명화되어 unique 제약 충돌 없음
- [x] WITHDRAWN 유저의 이메일로 새 가입 가능

---

## Step 2 — P0: UserHistory 이력 테이블 도입

> 회원가입, 정보 변경, 탈퇴, 상태 변경 등 유저 관련 액션을 기록하는 감사(audit) 테이블.
> Step 1의 탈퇴 익명화와 함께 사용하여 탈퇴 전 원본 정보를 이력으로 추적할 수 있다.

### 2-1. DB 마이그레이션
- [x] `V9__create_user_history.sql` 작성
  ```sql
  CREATE TABLE user_history (
      id          BIGINT       NOT NULL AUTO_INCREMENT,
      user_id     BIGINT       NOT NULL,
      action      VARCHAR(30)  NOT NULL,
      field       VARCHAR(50)  NULL,
      old_value   VARCHAR(500) NULL,
      new_value   VARCHAR(500) NULL,
      created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      CONSTRAINT fk_uh_user FOREIGN KEY (user_id) REFERENCES users(id),
      INDEX idx_user_history_user_id (user_id)
  );
  ```

### 2-2. 엔티티 & Enum
- [x] `UserAction` Enum 작성 (`SIGNUP`, `PROFILE_UPDATE`, `STATUS_CHANGE`, `WITHDRAWAL`)
- [x] `UserHistory` 엔티티 작성 (`id`, `user`, `action`, `field`, `oldValue`, `newValue`, `createdAt`)
- [x] `UserHistory.of(User, UserAction, field, oldValue, newValue)` 정적 팩토리
- [x] `UserHistoryRepository` 인터페이스 작성 (`findByUserOrderByCreatedAtDesc`)

### 2-3. RED: 테스트 작성
- [x] `UserServiceTest` — 가입 시 `UserHistory(SIGNUP)` 저장 검증
- [x] `UserServiceTest` — 탈퇴 시 `UserHistory(WITHDRAWAL)` + email 변경 이력 저장 검증

### 2-4. GREEN: 서비스에 이력 기록 통합
- [x] `UserService`에 `UserHistoryRepository` 의존성 추가
- [x] `signup()` 완료 후 `UserHistory.of(user, SIGNUP, null, null, null)` 저장
- [x] `withdraw()` 호출 시 각 필드 변경에 대해 이력 기록
  - `UserHistory.of(user, WITHDRAWAL, "email", 원본이메일, 익명화이메일)`
  - `UserHistory.of(user, WITHDRAWAL, "phone", 원본폰, 익명화폰)`
  - `UserHistory.of(user, WITHDRAWAL, "nickname", 원본닉네임, 익명화닉네임)`
  - `UserHistory.of(user, WITHDRAWAL, "status", "ACTIVE", "WITHDRAWN")`

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN
- [x] 가입 시 user_history에 SIGNUP 레코드 1건 저장
- [x] 탈퇴 시 user_history에 WITHDRAWAL 레코드 4건(email, phone, nickname, status) 저장
- [x] `UserHistory` 테이블에서 탈퇴 전 원본 이메일 조회 가능

---

## Step 3 — P0: DataIntegrityViolationException 처리 (refactor.md #2)

### 2-1. RED: 테스트 작성
- [x] `GlobalExceptionHandler` 테스트 — `DataIntegrityViolationException` 발생 시 `409 Conflict` + `DUPLICATE` 코드 반환 검증

### 2-2. GREEN: 구현
- [x] `GlobalExceptionHandler`에 `DataIntegrityViolationException` 핸들러 추가 (`409 Conflict`)

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN
- [x] DB unique 제약 위반 시 500 대신 `409 Conflict` 응답

---

## Step 4 — P1: RefreshToken 로그인 시 기존 토큰 정리 (refactor.md #5)

### 3-1. RED: 테스트 작성
- [x] `AuthServiceTest` — login 시 기존 RefreshToken이 revoke되는지 검증
- [x] `AuthServiceTest` — login 성공 시 새 RefreshToken이 저장되는지 verify

### 3-2. GREEN: 구현
- [x] `RefreshTokenRepository`에 `revokeAllByUser` 벌크 UPDATE 쿼리 추가 (`@Modifying` + `@Query`)
- [x] `AuthService.login()`에서 새 토큰 저장 전 `revokeAllByUser(user)` 호출

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN
- [x] 동일 유저가 재로그인하면 기존 RefreshToken이 `revoked = true`로 변경됨
- [x] 새 RefreshToken은 정상 저장됨

---

## Step 5 — P1: check-* API 유효성 검증 추가 (refactor.md #8)

### 4-1. RED: 테스트 작성
- [x] `AuthControllerTest` — `check-email?email=` 빈값 → `400` 검증
- [x] `AuthControllerTest` — `check-email?email=invalid` 형식 오류 → `400` 검증
- [x] `AuthControllerTest` — `check-phone?phone=12345` 형식 오류 → `400` 검증
- [x] `AuthControllerTest` — `check-nickname?nickname=a` 길이 미달 → `400` 검증

### 4-2. GREEN: 구현
- [x] `AuthController`에 `@Validated` 추가
- [x] `checkEmail` 파라미터에 `@NotBlank @Email` 추가
- [x] `checkPhone` 파라미터에 `@NotBlank @Pattern(regexp = "^010\\d{8}$")` 추가
- [x] `checkNickname` 파라미터에 `@NotBlank @Size(min = 2, max = 10)` 추가
- [x] `GlobalExceptionHandler`에 `ConstraintViolationException` 핸들러 추가 (`400`)
- [x] `GlobalExceptionHandler`에 `HandlerMethodValidationException` 핸들러 추가 (Spring 6.1+ 대응)

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN
- [x] `check-email?email=` → `400 VALIDATION_ERROR`
- [x] `check-email?email=valid@test.com` → `200 OK` (기존 동작 유지)

---

## Step 6 — P1: 관심 지역 중복 방지 (refactor.md #9)

### 5-1. RED: 테스트 작성
- [x] `UserServiceTest` — `interestRegions = ["유럽", "유럽", "동남아"]`로 가입 시 중복 제거되어 2건만 저장 검증

### 5-2. GREEN: 구현
- [x] `UserService.signup()`에서 `regions.stream().distinct()` 적용

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN
- [x] 중복 지역 포함 가입 요청 시 중복 제거 후 저장

---

## Step 7 — P2: JwtProvider SecretKey 캐싱 (refactor.md #3)

### 6-1. 구현
- [x] `JwtProvider` 생성자에서 `SecretKey`를 한 번만 생성하여 필드에 저장
- [x] `getSecretKey()` 메서드 제거, 필드 직접 참조
- [x] `@RequiredArgsConstructor` → 명시적 생성자로 변경

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN (`JwtProviderTest` 7개 통과)
- [x] `SecretKey`가 `final` 필드로 초기화 시 1회만 생성됨

---

## Step 8 — P2: SignupCommand 컨벤션 통일 (refactor.md #4)

### 7-1. 구현
- [x] `SignupCommand`의 `@RequiredArgsConstructor` → `@AllArgsConstructor(access = AccessLevel.PRIVATE)` 변경

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN
- [x] 프로젝트 내 모든 Command/DTO가 `@AllArgsConstructor(access = PRIVATE)` + 정적 팩토리 패턴 통일

---

## Step 9 — P2: SecurityConfig JSON 하드코딩 제거 (refactor.md #6)

### 8-1. 구현
- [x] `SecurityConfig`에 `ObjectMapper` 주입 (`@RequiredArgsConstructor`)
- [x] `authenticationEntryPoint`에서 `ApiResponse.error()` + `objectMapper.writeValueAsString()` 사용
- [x] `accessDeniedHandler`에서 동일 방식 적용
- [x] `UNAUTHORIZED_BODY`, `FORBIDDEN_BODY` 상수 제거

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN (`AdminUserControllerTest` 401/403 테스트 통과)
- [x] SecurityConfig에 수동 JSON 문자열이 없음

---

## Step 10 — P2: Clock PSA 적용 (refactor.md #7)

### 9-1. RED: 테스트 작성
- [x] `RefreshTokenTest` — 고정 시간 기반 `isExpired()`, `isUsable()` 검증

### 9-2. GREEN: 구현
- [x] `JpaConfig`에 `Clock` Bean 등록 (`Clock.systemDefaultZone()`)
- [x] `RefreshToken.isExpired(Clock)`, `isUsable(Clock)` 파라미터 추가
- [x] 기존 파라미터 없는 `isExpired()`, `isUsable()` 제거 (사용처가 아직 없으므로)
- [x] `AuthService`에 `Clock` 주입, login 시 `LocalDateTime.now(clock)` 사용

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN
- [x] `LocalDateTime.now()` 직접 호출이 서비스/엔티티 코드에서 제거됨
- [x] 테스트에서 `Clock.fixed()`로 시간 제어 가능

---

## Step 11 — P2: Cookie maxAge 설정값 연동 (refactor.md #10)

### 10-1. 구현
- [x] `AuthController`에 `JwtProperties` 의존성 추가
- [x] Cookie `maxAge(Duration.ofDays(30))` → `maxAge(Duration.ofSeconds(jwtProperties.getRefreshTokenExpiry()))` 변경

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN
- [x] `application.yaml`의 `refresh-token-expiry` 변경 시 Cookie maxAge도 자동 반영

---

## Step 12 — P3: 프로필 분리 (refactor.md #11)

### 11-1. 구현
- [x] `application.yaml`에서 `show-sql: true`, `format_sql: true` 제거 (기본값 false)
- [x] `application-dev.yaml` 생성: `show-sql: true`, `format_sql: true` 설정
- [x] `application.yaml`에 `spring.profiles.active: dev` 추가 (로컬 개발 기본)

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN
- [x] `application.yaml`에 `show-sql` 설정 없음
- [x] `application-dev.yaml`에 개발 전용 설정 분리

---

## Step 13 — P3: ApiResponse.ofPage() 레거시 제거 (refactor.md #12)

### 12-1. 구현
- [x] `ApiResponse.ofPage()` 사용처 검색 → 없으면 삭제
- [x] `ApiResponseTest`에 `ofPage` 관련 테스트가 있으면 함께 제거

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN
- [x] `ApiResponse`에 `ofPage()` 메서드 없음
- [x] Builder `data(Page<E>)` 오버로드가 유일한 페이지 처리 방식

---

## Step 14 — P3: 테스트 커버리지 보강 (refactor.md #13)

### 13-1. UserService 추가 테스트
- [x] `signup_관심지역_null이면_저장하지_않음`
- [x] `signup_관심지역_있으면_정상_저장`
- [x] `isEmailAvailable_사용가능_true`
- [x] `isEmailAvailable_이미_사용중_false`
- [x] `isPhoneAvailable_사용가능_true`
- [x] `isNicknameAvailable_사용가능_true`

### 13-2. AuthService 추가 테스트
- [x] `login_성공시_RefreshToken_save_호출_검증` (verify)

### 13-3. AuthController 추가 테스트
- [x] `login_Set_Cookie_Secure_SameSite_Path_검증`

### 13-4. GlobalExceptionHandler 테스트
- [x] `handleException_500_에러_반환` (예상치 못한 RuntimeException)

### 수정 완료 기준
- [x] `./gradlew test` 전체 GREEN
- [x] 신규 테스트 최소 10개 추가
- [x] `UserService` check 메서드, `AuthService` save verify, `GlobalExceptionHandler` 500 경로 커버

---

## 전체 완료 기준

- [x] `./gradlew test` 전체 GREEN
- [x] refactor.md의 13개 이슈 + UserHistory 이력 테이블 모두 해소
- [x] P0 이슈(ACTIVE 유저 삭제, 500 에러 노출) 완전 해결
- [x] 탈퇴 시 레코드 보존 + 고유 필드 익명화 + UserHistory 이력 기록
- [x] 프로젝트 내 DTO 컨벤션 통일
- [x] `LocalDateTime.now()` 직접 호출 제거 (PSA Clock 적용)
- [x] SecurityConfig에 수동 JSON 문자열 제거
