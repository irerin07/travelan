# Travelan

여행자들이 여행 경험을 공유하고 소통하는 커뮤니티 플랫폼의 백엔드 API 서버.

---

## 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 |
| Framework | Spring Boot | 4.0.5 |
| ORM | Spring Data JPA + Hibernate | - |
| Query | QueryDSL | 5.1.0 |
| Validation | Jakarta Bean Validation | - |
| Security | Spring Security | - |
| Auth | JWT (jjwt) | 0.12.6 |
| Password | BCrypt (cost factor 10) | - |
| Database | MySQL | 8.4 |
| Migration | Flyway | - |
| Build | Gradle | 9.4.0 |
| Container | Docker + Docker Compose | - |

---

## 로컬 개발 환경 실행

### 사전 요구 사항

- Java 21
- Docker & Docker Compose

### 실행

```bash
# MySQL 컨테이너 시작
docker-compose up -d

# 애플리케이션 실행
./gradlew bootRun
```

```bash
# 헬스 체크
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

```bash
# 테스트
./gradlew test
```

### Docker Compose 구성

| 서비스 | 포트 | 설명 |
|--------|------|------|
| MySQL 8.4 | 3306 | DB (database: `travelan`, user: `travelan`) |
| App | 8080 | Spring Boot 애플리케이션 |

---

## 패키지 구조

```
com.irerin.travelan
├── auth
│   ├── controller       # AuthController (회원가입, 로그인, 토큰 갱신, 로그아웃)
│   ├── dto              # SignupRequest, SignupResponse, AvailableResponse
│   ├── entity           # RefreshToken
│   ├── jwt              # JwtProvider, JwtAuthenticationFilter
│   ├── repository       # RefreshTokenRepository
│   ├── service
│   └── validation       # @PasswordMatch 커스텀 검증 어노테이션
├── user
│   ├── controller       # AdminUserController (관리자 전용 회원 목록)
│   ├── dto              # SignupCommand (서비스 레이어 DTO)
│   ├── entity           # User, UserInterestRegion, UserRole, UserStatus
│   ├── repository       # UserRepository, UserInterestRegionRepository
│   └── service          # UserService
├── admin
│   └── dto              # UserSummaryResponse
├── common
│   ├── config           # SecurityConfig, JpaConfig
│   ├── exception        # GlobalExceptionHandler, DuplicateException, AuthException
│   └── response         # ApiResponse, ErrorResponse, PageMeta
└── TravelanApplication.java
```

---

## API 명세

### 인증 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/v1/auth/signup` | 회원가입 | 불필요 |
| POST | `/api/v1/auth/login` | 로그인 | 불필요 |
| POST | `/api/v1/auth/logout` | 로그아웃 | Access Token |
| POST | `/api/v1/auth/refresh` | 토큰 갱신 | Refresh Token (Cookie) |
| GET | `/api/v1/auth/check-email` | 이메일 중복 확인 | 불필요 |
| GET | `/api/v1/auth/check-phone` | 핸드폰 번호 중복 확인 | 불필요 |
| GET | `/api/v1/auth/check-nickname` | 닉네임 중복 확인 | 불필요 |

### 관리자 API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/v1/admin/users` | 전체 회원 목록 조회 (페이징) | ADMIN 역할 |

---

### 회원가입 `POST /api/v1/auth/signup`

**Request**
```json
{
  "email": "user@example.com",
  "password": "P@ssw0rd!",
  "passwordConfirm": "P@ssw0rd!",
  "name": "홍길동",
  "phone": "01012345678",
  "nickname": "여행자123",
  "interestRegions": ["유럽", "동남아시아"]
}
```

**Response** `201 Created`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "여행자123"
}
```

### 로그인 `POST /api/v1/auth/login`

**Request**
```json
{
  "email": "user@example.com",
  "password": "P@ssw0rd!"
}
```

**Response** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

> Refresh Token은 `HttpOnly; Secure; SameSite=Strict` Cookie로 전달

### 전체 회원 목록 `GET /api/v1/admin/users`

**Query Parameters:** `page` (기본값: 1), `size` (기본값: 20)

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "email": "user@example.com",
      "name": "홍길동",
      "phone": "01012345678",
      "nickname": "여행자",
      "status": "ACTIVE",
      "role": "USER",
      "createdAt": "2026-03-31T10:00:00"
    }
  ],
  "page": {
    "page": 1,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

## 공통 응답 구조

### 성공
```json
{ "success": true, "data": { ... } }
```

### 성공 (페이징)
```json
{
  "success": true,
  "data": [ ... ],
  "page": { "page": 1, "size": 20, "totalElements": 100, "totalPages": 5 }
}
```

### 실패
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력값이 올바르지 않습니다",
    "errors": [
      { "field": "email", "message": "올바른 이메일 형식이 아닙니다" }
    ]
  }
}
```

### HTTP 상태 코드

| 코드 | 상황 |
|------|------|
| 201 | 회원가입 성공 |
| 200 | 로그인 / 토큰 갱신 / 조회 성공 |
| 204 | 로그아웃 성공 |
| 400 | 입력값 유효성 실패 |
| 401 | 인증 실패 / 토큰 없음 또는 만료 |
| 403 | 권한 없음 |
| 409 | 이메일 / 핸드폰 / 닉네임 중복 |
| 429 | 로그인 5회 연속 실패 → 계정 잠금 |

---

## 인증 구조

- **Access Token**: JWT, 유효기간 1시간, `Authorization: Bearer {token}` 헤더
- **Refresh Token**: JWT, 유효기간 30일, `HttpOnly Cookie`, DB 저장
- **Refresh Token Rotation**: 토큰 갱신 시 새 Refresh Token 발급, 재사용 감지 시 전체 세션 무효화

---

## DB 마이그레이션 (Flyway)

| 파일 | 내용 |
|------|------|
| `V1__create_users.sql` | `users` 테이블 생성 |
| `V2__create_user_interest_region.sql` | `user_interest_region` 테이블 생성 |
| `V3__create_refresh_token.sql` | `refresh_token` 테이블 생성 |
| `V5__add_role_to_users.sql` | `users` 테이블에 `role` 컬럼 추가 |

> V4는 Phase 5 (로그인 실패 횟수 추적) 용으로 예약

---

## 개발 로드맵

| Phase | 내용 | 상태 |
|-------|------|------|
| 1 | 기반 구축 (엔티티, DB 마이그레이션, 공통 응답/예외) | 완료 |
| 2 | 회원가입 (유효성 검증, 중복 확인, BCrypt 해싱) | 완료 |
| 2.5 | 관리자 회원 목록 조회 API (RBAC, 페이징) | 완료 |
| 3 | 로그인 & JWT (토큰 발급, 인증 필터) | 예정 |
| 4 | 토큰 갱신 / 로그아웃 (Refresh Token Rotation) | 예정 |
| 5 | 로그인 보안 강화 (Brute Force 방어, 계정 잠금) | 예정 |
