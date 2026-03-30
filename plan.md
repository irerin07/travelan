# Travelan - 회원가입 / 로그인 개발 계획

## 기술 스택

| 분류 | 기술 | 버전 | 비고 |
|------|------|------|------|
| Language | Java | 21 | |
| Framework | Spring Boot | 4.0.5 | |
| ORM | Spring Data JPA + Hibernate | - | |
| Query | QueryDSL | 5.1.0 | 복잡한 동적 쿼리용 |
| Validation | Jakarta Bean Validation | - | `@Valid` 기반 입력값 검증 |
| Security | Spring Security | - | 인증/인가 필터 체인 |
| Auth | JWT (jjwt) | 0.12.x | Access Token / Refresh Token |
| Password | BCrypt | - | Spring Security 내장 |
| Database | MySQL | 8.4 | |
| Build | Gradle | 9.4.0 | |
| Container | Docker + Docker Compose | - | 로컬 개발 환경 |
| Boilerplate | Lombok | - | |

### 인증 방식
- **Access Token**: JWT, 유효기간 1시간, `Authorization: Bearer {token}` 헤더로 전달
- **Refresh Token**: JWT, 유효기간 30일, `HttpOnly Cookie`로 전달, DB에 저장하여 유효성 관리
- **비밀번호**: BCrypt (cost factor 10) 해싱 후 저장

### 패키지 구조

```
com.irerin.travelan
├── auth
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── jwt
├── user
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
├── common
│   ├── exception
│   ├── response
│   └── config
└── TravelanApplication.java
```

---

## Phase 1 — 기반 구축

> 비즈니스 로직 없이 프로젝트가 정상 기동되고, DB 연결과 엔티티 구조가 완성된 상태

### 작업 목록

#### 1-1. 의존성 추가 (`build.gradle`)
- `spring-boot-starter-security`
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson`

#### 1-2. DB 설정 (`application.yaml`)
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/travelan
    username: travelan
    password: travelan
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
  flyway:
    enabled: true
```

#### 1-3. 공통 응답 구조 정의
```java
// ApiResponse<T> — 성공/실패 공통 래퍼
{
  "success": true,
  "data": { ... },
  "error": null
}
```

#### 1-4. 글로벌 예외 핸들러
- `@RestControllerAdvice` 기반 `GlobalExceptionHandler`
- `400 Bad Request`: 유효성 검증 실패
- `401 Unauthorized`: 인증 실패
- `409 Conflict`: 중복 데이터

#### 1-5. 엔티티 설계 및 DB 마이그레이션 (Flyway)

**V1__create_users.sql**
```sql
CREATE TABLE users (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  email      VARCHAR(255) NOT NULL,
  password   VARCHAR(255) NOT NULL,
  name       VARCHAR(50)  NOT NULL,
  phone      VARCHAR(20)  NOT NULL,
  nickname   VARCHAR(10)  NOT NULL,
  status     ENUM('ACTIVE','SUSPENDED','WITHDRAWN') NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email    (email),
  UNIQUE KEY uk_users_phone    (phone),
  UNIQUE KEY uk_users_nickname (nickname)
);
```

**V2__create_user_interest_region.sql**
```sql
CREATE TABLE user_interest_region (
  id        BIGINT       NOT NULL AUTO_INCREMENT,
  user_id BIGINT       NOT NULL,
  region    VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_uir_user FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**V3__create_refresh_token.sql**
```sql
CREATE TABLE refresh_token (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id  BIGINT       NOT NULL,
  token      VARCHAR(512) NOT NULL,
  expires_at DATETIME     NOT NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_refresh_token (token),
  CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 완료 기준
- `./gradlew bootRun` 정상 기동
- DB 테이블 자동 생성 확인
- `/actuator/health` → `{"status":"UP"}`

---

## Phase 2 — 회원가입

> 이메일·비밀번호·실명·핸드폰 번호·닉네임으로 계정을 생성할 수 있다

### 작업 목록

#### 2-1. Spring Security 기본 설정
- 모든 `/api/v1/auth/**` 엔드포인트 인증 없이 허용
- CSRF 비활성화 (REST API)
- Session Stateless 설정

#### 2-2. `POST /api/v1/auth/signup`
| 항목 | 내용 |
|------|------|
| Request | `email`, `password`, `passwordConfirm`, `name`, `phone`, `nickname`, `interestRegions` |
| Response | `201 Created` — `id`, `email`, `nickname` |

**유효성 규칙 (Jakarta Validation)**
- `email`: `@Email`, `@NotBlank`
- `password`: `@NotBlank`, 최소 8자, 공백 포함 불가 (`@Pattern`)
- `passwordConfirm`: 비밀번호와 일치 (커스텀 `@PasswordMatch` 어노테이션)
- `name`: `@NotBlank`
- `phone`: `@Pattern(regexp = "^010\\d{8}$")`
- `nickname`: `@NotBlank`, 2~10자, 공백 포함 불가 (`@Pattern`)

**처리 흐름**
1. `@Valid` 유효성 검증
2. 이메일/핸드폰/닉네임 중복 확인 → 중복 시 `409 Conflict`
3. 비밀번호 BCrypt 해싱
4. `User` + `UserInterestRegion` 저장
5. `201 Created` 반환

#### 2-3. 중복 확인 API
- `GET /api/v1/auth/check-email?email={email}`
- `GET /api/v1/auth/check-phone?phone={phone}`
- `GET /api/v1/auth/check-nickname?nickname={nickname}`

응답: `{ "available": true }`

### 완료 기준
- 정상 가입 → `201 Created`
- 중복 이메일/핸드폰/닉네임 → `409 Conflict`
- 유효성 위반 → `400 Bad Request` (필드별 에러 메시지 포함)

---

## Phase 3 — 로그인 & JWT 발급

> 이메일/비밀번호로 로그인하여 Access Token과 Refresh Token을 발급받는다

### 작업 목록

#### 3-1. JWT 유틸리티 (`JwtProvider`)
- Access Token 생성 (subject: `userId`, 만료: 1시간)
- Refresh Token 생성 (만료: 30일)
- 토큰 검증 / Claims 파싱

#### 3-2. `POST /api/v1/auth/login`
| 항목 | 내용 |
|------|------|
| Request | `email`, `password` |
| Response | `200 OK` — `accessToken`, `tokenType: "Bearer"`, `expiresIn: 3600` |

**처리 흐름**
1. 이메일로 회원 조회 → 미존재 시 `401`
2. BCrypt 비밀번호 검증 → 불일치 시 `401`
3. Access Token 생성
4. Refresh Token 생성 → DB 저장
5. Refresh Token → `HttpOnly; Secure; SameSite=Strict` Cookie 설정
6. Access Token 반환

**실패 응답**: 이메일/비밀번호 구분 없이 `"이메일 또는 비밀번호가 올바르지 않습니다"` 단일 메시지

#### 3-3. JWT 인증 필터 (`JwtAuthenticationFilter`)
- `Authorization: Bearer {token}` 헤더 파싱
- 유효한 토큰이면 `SecurityContextHolder`에 인증 정보 설정
- 인증 필요 API에서 토큰 없거나 유효하지 않으면 `401`

### 완료 기준
- 정상 로그인 → Access Token 반환 + Refresh Token Cookie 설정
- 잘못된 자격증명 → `401`
- 발급된 Access Token으로 인증 필요 API 호출 성공

---

## Phase 4 — 토큰 갱신 & 로그아웃

> Refresh Token으로 Access Token을 갱신하고, 로그아웃 시 세션을 안전하게 종료한다

### 작업 목록

#### 4-1. `POST /api/v1/auth/refresh`
- Cookie의 Refresh Token 추출
- DB에서 토큰 존재 여부 및 만료 확인
- 유효하면 새 Access Token 발급
- **Refresh Token Rotation**: 새 Refresh Token 발급 후 기존 토큰 교체
- 이미 사용된 토큰 재사용 감지 시 → 해당 회원의 모든 Refresh Token 무효화 (`401`)

#### 4-2. `POST /api/v1/auth/logout`
- DB에서 Refresh Token 삭제
- `Set-Cookie` 헤더로 Cookie 만료 처리 (Max-Age=0)
- `204 No Content` 반환

### 완료 기준
- 유효한 Refresh Token → 새 Access Token 반환
- 만료/없는 Refresh Token → `401`
- 로그아웃 후 Refresh Token 재사용 시도 → `401`

---

## Phase 5 — 로그인 보안 강화

> 무차별 대입 공격(Brute Force) 방어

### 작업 목록

#### 5-1. 로그인 실패 횟수 추적
- `User` 테이블에 `login_fail_count INT`, `locked_until DATETIME` 컬럼 추가 (V4 마이그레이션)
- 로그인 실패 시 `login_fail_count` 증가
- 5회 연속 실패 시 `locked_until = now() + 30분` 설정

#### 5-2. 잠금 상태 확인
- 로그인 시도 시 `locked_until` 확인 → 잠금 중이면 `429 Too Many Requests`
- 잠금 해제 시간 응답에 포함: `"retryAfter": "2026-03-30T12:30:00"`
- 로그인 성공 시 `login_fail_count = 0`, `locked_until = null` 초기화

### 완료 기준
- 5회 실패 후 → `429 Too Many Requests` + 잠금 해제 시간 반환
- 30분 후 → 로그인 정상 가능
- 성공 시 → 실패 카운트 초기화

---

## 개발 순서 요약

```
Phase 1  기반 구축          엔티티, DB 마이그레이션, 공통 응답/예외 구조
   ↓
Phase 2  회원가입           입력 유효성, 중복 확인, BCrypt 해싱, 저장
   ↓
Phase 3  로그인 & JWT       토큰 발급, 인증 필터, Security 설정
   ↓
Phase 4  토큰 갱신/로그아웃  Refresh Token Rotation, 세션 종료
   ↓
Phase 5  보안 강화          로그인 실패 제한, 계정 잠금
```
