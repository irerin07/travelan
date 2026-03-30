# Travelan - 구현 태스크

---

## Phase 1 — 기반 구축

### 1-1. 의존성 추가
- [ ] `build.gradle`에 `spring-boot-starter-security` 추가
- [ ] `build.gradle`에 `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.12.x) 추가
- [ ] `build.gradle`에 `flyway-core`, `flyway-mysql` 추가
- [ ] Gradle 빌드 정상 완료 확인

### 1-2. DB 및 JPA 설정
- [ ] `application.yaml`에 datasource (MySQL) 설정
- [ ] `application.yaml`에 JPA `ddl-auto: validate` 설정
- [ ] `application.yaml`에 Flyway `enabled: true` 설정

### 1-3. 패키지 구조 생성
- [ ] `auth.controller` / `auth.service` / `auth.repository` / `auth.dto` / `auth.jwt` 패키지 생성
- [ ] `member.controller` / `member.service` / `member.repository` / `member.dto` / `member.entity` 패키지 생성
- [ ] `common.exception` / `common.response` / `common.config` 패키지 생성

### 1-4. 공통 응답 구조
- [ ] `ApiResponse<T>` 클래스 작성 (`success`, `data`, `error` 필드)
- [ ] 성공 응답 정적 팩토리 메서드 (`ApiResponse.ok(data)`)
- [ ] 실패 응답 정적 팩토리 메서드 (`ApiResponse.error(code, message)`)
- [ ] `ErrorResponse` 클래스 작성 (에러 코드, 메시지, 필드별 에러 목록)

### 1-5. 글로벌 예외 핸들러
- [ ] `GlobalExceptionHandler` (`@RestControllerAdvice`) 클래스 작성
- [ ] `MethodArgumentNotValidException` 처리 → `400 Bad Request` + 필드별 에러
- [ ] `DuplicateException` (커스텀) 처리 → `409 Conflict`
- [ ] `AuthException` (커스텀) 처리 → `401 Unauthorized`
- [ ] 그 외 미처리 예외 → `500 Internal Server Error`

### 1-6. Flyway 마이그레이션 스크립트
- [ ] `V1__create_member.sql` 작성 및 적용 확인
- [ ] `V2__create_member_interest_region.sql` 작성 및 적용 확인
- [ ] `V3__create_refresh_token.sql` 작성 및 적용 확인

### 1-7. 엔티티 클래스
- [ ] `Member` 엔티티 작성 (`id`, `email`, `password`, `name`, `phone`, `nickname`, `status`, `createdAt`, `updatedAt`)
- [ ] `MemberStatus` Enum 작성 (`ACTIVE`, `SUSPENDED`, `WITHDRAWN`)
- [ ] `MemberInterestRegion` 엔티티 작성 (`id`, `memberId`, `region`)
- [ ] `RefreshToken` 엔티티 작성 (`id`, `memberId`, `token`, `expiresAt`, `createdAt`)

### 1-8. Phase 1 완료 기준 검증
- [ ] `./gradlew bootRun` 정상 기동
- [ ] Flyway 마이그레이션으로 테이블 3개 생성 확인
- [ ] `/actuator/health` → `{"status":"UP"}` 응답 확인

---

## Phase 2 — 회원가입

### 2-1. Spring Security 기본 설정
- [ ] `SecurityConfig` 클래스 작성
- [ ] `/api/v1/auth/**` 인증 없이 허용 설정
- [ ] CSRF 비활성화 설정
- [ ] Session `STATELESS` 설정
- [ ] `PasswordEncoder` Bean 등록 (BCrypt, cost 10)

### 2-2. 커스텀 유효성 어노테이션
- [ ] `@PasswordMatch` 어노테이션 및 `PasswordMatchValidator` 작성 (비밀번호 확인 일치 검증)
- [ ] `@NoWhitespace` 어노테이션 또는 `@Pattern`으로 공백 불가 규칙 정의

### 2-3. 회원가입 DTO
- [ ] `SignupRequest` 작성 — 필드별 `@Valid` 어노테이션 적용
  - [ ] `email`: `@NotBlank`, `@Email`
  - [ ] `password`: `@NotBlank`, 8자 이상, 공백 불가 (`@Pattern`)
  - [ ] `passwordConfirm`: `@NotBlank`, `@PasswordMatch`
  - [ ] `name`: `@NotBlank`
  - [ ] `phone`: `@NotBlank`, `@Pattern(regexp = "^010\\d{8}$")`
  - [ ] `nickname`: `@NotBlank`, 2~10자, 공백 불가 (`@Pattern`)
  - [ ] `interestRegions`: `@Size(max = 5)`
- [ ] `SignupResponse` 작성 (`id`, `email`, `nickname`)

### 2-4. 회원 Repository
- [ ] `MemberRepository` 인터페이스 작성
  - [ ] `existsByEmail(String email)`
  - [ ] `existsByPhone(String phone)`
  - [ ] `existsByNicknameIgnoreCase(String nickname)`
  - [ ] `findByEmail(String email)`
- [ ] `MemberInterestRegionRepository` 인터페이스 작성

### 2-5. 회원가입 Service
- [ ] `MemberService` 클래스 작성
- [ ] 이메일 중복 확인 로직
- [ ] 핸드폰 번호 중복 확인 로직
- [ ] 닉네임 중복 확인 로직 (대소문자 무시)
- [ ] 비밀번호 BCrypt 해싱
- [ ] `Member` 저장
- [ ] `MemberInterestRegion` 리스트 저장 (관심 지역 선택 시)

### 2-6. 회원가입 Controller
- [ ] `AuthController` 클래스 작성
- [ ] `POST /api/v1/auth/signup` → `201 Created` 구현

### 2-7. 중복 확인 API
- [ ] `GET /api/v1/auth/check-email?email={email}` 구현
- [ ] `GET /api/v1/auth/check-phone?phone={phone}` 구현
- [ ] `GET /api/v1/auth/check-nickname?nickname={nickname}` 구현
- [ ] 응답 형식: `{ "available": true/false }`

### 2-8. Phase 2 완료 기준 검증
- [ ] 정상 가입 요청 → `201 Created` 확인
- [ ] 중복 이메일 → `409 Conflict` 확인
- [ ] 중복 핸드폰 번호 → `409 Conflict` 확인
- [ ] 중복 닉네임 → `409 Conflict` 확인
- [ ] 비밀번호 공백 포함 → `400 Bad Request` 확인
- [ ] 닉네임 11자 이상 → `400 Bad Request` 확인
- [ ] 중복 확인 API 정상 동작 확인

---

## Phase 3 — 로그인 & JWT 발급

### 3-1. JWT 설정
- [ ] `application.yaml`에 JWT secret key, access-token 만료, refresh-token 만료 설정
- [ ] `JwtProperties` (`@ConfigurationProperties`) 클래스 작성

### 3-2. JwtProvider
- [ ] `JwtProvider` 클래스 작성
- [ ] Access Token 생성 메서드 (subject: `memberId`, 만료: 1시간)
- [ ] Refresh Token 생성 메서드 (만료: 30일)
- [ ] 토큰 유효성 검증 메서드
- [ ] 토큰에서 `memberId` 파싱 메서드
- [ ] 토큰 만료 여부 확인 메서드

### 3-3. RefreshToken Repository
- [ ] `RefreshTokenRepository` 인터페이스 작성
  - [ ] `findByToken(String token)`
  - [ ] `findByMemberId(Long memberId)`
  - [ ] `deleteByMemberId(Long memberId)`

### 3-4. 로그인 DTO
- [ ] `LoginRequest` 작성 (`email`, `password`)
- [ ] `LoginResponse` 작성 (`accessToken`, `tokenType`, `expiresIn`)

### 3-5. 로그인 Service
- [ ] `AuthService` 클래스 작성
- [ ] 이메일로 회원 조회 → 미존재 시 `AuthException` 발생
- [ ] BCrypt 비밀번호 검증 → 불일치 시 `AuthException` 발생
- [ ] Access Token 생성
- [ ] Refresh Token 생성 → DB 저장
- [ ] Refresh Token 반환

### 3-6. 로그인 Controller
- [ ] `POST /api/v1/auth/login` 구현
- [ ] Refresh Token → `HttpOnly; Secure; SameSite=Strict` Cookie 설정
- [ ] Access Token → 응답 body 반환
- [ ] 실패 시 이메일/비밀번호 구분 없는 단일 에러 메시지 반환

### 3-7. JWT 인증 필터
- [ ] `JwtAuthenticationFilter` (`OncePerRequestFilter`) 작성
- [ ] `Authorization: Bearer {token}` 헤더 추출 로직
- [ ] 유효한 토큰이면 `UsernamePasswordAuthenticationToken` 생성 후 `SecurityContextHolder` 설정
- [ ] 무효/만료 토큰 → 필터 체인 통과 (컨트롤러에서 `401` 처리)
- [ ] `SecurityConfig`에 필터 등록 (`addFilterBefore`)

### 3-8. Phase 3 완료 기준 검증
- [ ] 정상 로그인 → `200 OK` + Access Token 반환 확인
- [ ] 정상 로그인 → Refresh Token Cookie `Set-Cookie` 헤더 확인
- [ ] 잘못된 이메일 → `401` 단일 메시지 확인
- [ ] 잘못된 비밀번호 → `401` 단일 메시지 확인
- [ ] 발급받은 Access Token으로 인증 필요 API 호출 성공 확인
- [ ] 토큰 없이 인증 필요 API 호출 → `401` 확인

---

## Phase 4 — 토큰 갱신 & 로그아웃

### 4-1. 토큰 갱신 Service
- [ ] `AuthService`에 `refresh()` 메서드 추가
- [ ] Cookie에서 Refresh Token 추출
- [ ] DB에서 Refresh Token 존재 여부 확인 → 없으면 `401`
- [ ] Refresh Token 만료 여부 확인 → 만료 시 `401`
- [ ] 새 Access Token 생성
- [ ] Refresh Token Rotation: 새 Refresh Token 발급 → 기존 토큰 교체 (DB 업데이트)
- [ ] 재사용 감지 (이미 삭제된 토큰으로 요청 시) → 해당 회원 전체 Refresh Token 삭제 + `401`

### 4-2. 토큰 갱신 Controller
- [ ] `POST /api/v1/auth/refresh` 구현
- [ ] 새 Access Token → 응답 body 반환
- [ ] 새 Refresh Token → Cookie 갱신

### 4-3. 로그아웃 Service
- [ ] `AuthService`에 `logout()` 메서드 추가
- [ ] Access Token에서 `memberId` 파싱
- [ ] DB에서 해당 회원의 Refresh Token 전체 삭제

### 4-4. 로그아웃 Controller
- [ ] `POST /api/v1/auth/logout` 구현
- [ ] `Set-Cookie` 헤더로 Refresh Token Cookie 만료 처리 (`Max-Age=0`)
- [ ] `204 No Content` 반환

### 4-5. Phase 4 완료 기준 검증
- [ ] 유효한 Refresh Token → 새 Access Token 반환 확인
- [ ] Rotation으로 새 Refresh Token Cookie 갱신 확인
- [ ] 만료된 Refresh Token → `401` 확인
- [ ] 로그아웃 → `204 No Content` + Cookie 삭제 확인
- [ ] 로그아웃 후 기존 Refresh Token 재사용 → `401` 확인
- [ ] 이미 사용된 Refresh Token 재사용 시 전체 세션 무효화 확인

---

## Phase 5 — 로그인 보안 강화

### 5-1. DB 마이그레이션
- [ ] `V4__add_login_fail_columns.sql` 작성 (`login_fail_count INT DEFAULT 0`, `locked_until DATETIME NULL`)
- [ ] 마이그레이션 적용 확인
- [ ] `Member` 엔티티에 `loginFailCount`, `lockedUntil` 필드 추가

### 5-2. 계정 잠금 로직
- [ ] 로그인 시도 시 `lockedUntil` 확인 → 잠금 중이면 `429 Too Many Requests` + `retryAfter` 반환
- [ ] 비밀번호 불일치 시 `loginFailCount` 증가
- [ ] `loginFailCount >= 5` 시 `lockedUntil = now() + 30분` 설정
- [ ] 로그인 성공 시 `loginFailCount = 0`, `lockedUntil = null` 초기화

### 5-3. Phase 5 완료 기준 검증
- [ ] 비밀번호 5회 연속 실패 → `429 Too Many Requests` + `retryAfter` 필드 확인
- [ ] 잠금 상태에서 로그인 시도 → `429` 반환 확인
- [ ] 30분 경과 후 정상 로그인 가능 확인
- [ ] 로그인 성공 후 실패 카운트 초기화 확인
