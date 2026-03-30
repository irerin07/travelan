# Travelan - 회원가입 / 로그인 PRD

## 1. 개요

Travelan은 여행자들이 여행 경험을 공유하고 소통하는 커뮤니티 플랫폼이다.
본 문서는 서비스의 첫 번째 기능인 **회원가입**과 **로그인**에 대한 요구사항을 정의한다.

---

## 2. 목표

| 목표 | 설명 |
|------|------|
| 신뢰할 수 있는 회원 체계 구축 | 이메일 기반 고유 계정으로 회원 관리 |
| 안전한 인증 | JWT 기반 Stateless 인증으로 보안성 확보 |
| 확장 가능한 구조 | 추후 소셜 로그인 / 본인 인증 통합이 용이한 구조 |

---

## 3. 사용자 스토리

### 회원가입
- 새 방문자로서, 이메일과 비밀번호로 계정을 생성하고 싶다.
- 가입 시 실명과 핸드폰 번호를 입력하여 신뢰할 수 있는 회원임을 증명하고 싶다.
- 가입 시 닉네임을 설정하여 커뮤니티에서 나를 표현하고 싶다.
- 관심 있는 여행 지역을 선택하여 맞춤 콘텐츠를 받고 싶다.

### 로그인 / 세션 관리
- 이미 가입한 회원으로서, 이메일/비밀번호로 빠르게 로그인하고 싶다.
- 로그인 상태가 유지되어 매번 로그인하지 않아도 되길 원한다.
- 보안을 위해 명시적으로 로그아웃할 수 있어야 한다.

---

## 4. 기능 요구사항

### 4.1 회원가입

#### 입력 항목

| 필드 | 타입 | 필수 여부 | 유효성 규칙 |
|------|------|-----------|-------------|
| 이메일 | string | 필수 | RFC 5322 이메일 형식, 중복 불가 |
| 비밀번호 | string | 필수 | 8자 이상, 공백 사용 불가 |
| 비밀번호 확인 | string | 필수 | 비밀번호와 일치 |
| 실명 | string | 필수 | 한글 또는 영문, 공백 제외 1자 이상 |
| 핸드폰 번호 | string | 필수 | 숫자만 허용, 한국 휴대폰 번호 형식 (010-XXXX-XXXX), 중복 불가 |
| 닉네임 | string | 필수 | 2~10자, 한글/영문/숫자, 공백 사용 불가, 중복 불가 |
| 여행 관심 지역 | list | 선택 | 최대 5개 선택 |

#### 처리 흐름
1. 클라이언트가 회원가입 요청 전송
2. 이메일 중복 확인
3. 핸드폰 번호 중복 확인
4. 닉네임 중복 확인
5. 비밀번호 해싱 (BCrypt)
6. 회원 정보 저장
7. 가입 완료 응답 반환

#### 제약 사항
- 동일 이메일로 중복 가입 불가
- 동일 핸드폰 번호로 중복 가입 불가
- 동일 닉네임 중복 불가 (대소문자 구분 없이)
- 비밀번호는 평문 저장 금지
- 비밀번호에 공백 포함 불가
- 닉네임에 공백 포함 불가

> **Note:** 추후 통합 본인 인증(PASS 등) 연동 예정. 현재 단계에서는 이메일 인증 없이 즉시 가입 완료.

---

### 4.2 로그인

#### 입력 항목

| 필드 | 타입 | 필수 여부 |
|------|------|-----------|
| 이메일 | string | 필수 |
| 비밀번호 | string | 필수 |

#### 처리 흐름
1. 이메일로 회원 조회
2. 비밀번호 검증 (BCrypt 비교)
3. Access Token 발급 (JWT)
4. Refresh Token 발급 및 저장
5. 토큰 반환

#### 토큰 정책

| 토큰 | 유효 기간 | 저장 위치 | 설명 |
|------|-----------|-----------|------|
| Access Token | 1시간 | 클라이언트 메모리 | API 호출 시 Authorization 헤더에 포함 |
| Refresh Token | 30일 | HttpOnly Cookie | Access Token 갱신에 사용 |

#### 실패 처리
- 이메일 미존재: `401 Unauthorized` (보안상 "이메일 또는 비밀번호가 틀렸습니다" 통일 메시지)
- 비밀번호 불일치: `401 Unauthorized` (동일 메시지)
- 5회 연속 실패 시: 계정 잠금 (30분), `429 Too Many Requests`

---

### 4.3 토큰 갱신

- Refresh Token이 유효한 경우 새 Access Token 발급
- Refresh Token 재사용 감지 시 해당 계정의 모든 Refresh Token 무효화 (토큰 탈취 대응)

---

### 4.4 로그아웃

- 서버에 저장된 Refresh Token 삭제
- 클라이언트의 HttpOnly Cookie 만료 처리

---

### 4.5 중복 확인 (가입 전 실시간 검증)

- 이메일 중복 확인 API (가입 폼 이탈 시 호출)
- 핸드폰 번호 중복 확인 API (입력 완료 시 호출)
- 닉네임 중복 확인 API (입력 완료 시 호출)

---

## 5. 비기능 요구사항

| 항목 | 요구사항 |
|------|----------|
| 성능 | 로그인 API 응답 시간 500ms 이내 |
| 보안 | HTTPS 통신 필수 |
| 보안 | 비밀번호 BCrypt(cost 10) 해싱 |
| 보안 | JWT 서명 알고리즘 HS256 이상 |
| 확장성 | 소셜 로그인 추가를 고려한 인증 구조 설계 |

---

## 6. 데이터 모델

### Member (회원)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 회원 고유 ID |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 로그인용 이메일 |
| password | VARCHAR(255) | NOT NULL | BCrypt 해시값 |
| name | VARCHAR(50) | NOT NULL | 실명 |
| phone | VARCHAR(20) | UNIQUE, NOT NULL | 핸드폰 번호 (숫자만 저장, 예: 01012345678) |
| nickname | VARCHAR(10) | UNIQUE, NOT NULL | 닉네임 |
| status | ENUM | NOT NULL | ACTIVE / SUSPENDED / WITHDRAWN |
| created_at | DATETIME | NOT NULL | 가입일시 |
| updated_at | DATETIME | NOT NULL | 수정일시 |

### MemberInterestRegion (여행 관심 지역)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK | |
| member_id | BIGINT | FK(Member) | |
| region | VARCHAR(100) | NOT NULL | 관심 여행 지역명 |

### RefreshToken

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK | |
| member_id | BIGINT | FK(Member) | |
| token | VARCHAR(512) | UNIQUE, NOT NULL | Refresh Token 값 |
| expires_at | DATETIME | NOT NULL | 만료일시 |
| created_at | DATETIME | NOT NULL | 발급일시 |

---

## 7. API 명세

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | /api/v1/auth/signup | 회원가입 | 불필요 |
| POST | /api/v1/auth/login | 로그인 | 불필요 |
| POST | /api/v1/auth/logout | 로그아웃 | Access Token |
| POST | /api/v1/auth/refresh | 토큰 갱신 | Refresh Token (Cookie) |
| GET | /api/v1/auth/check-email | 이메일 중복 확인 | 불필요 |
| GET | /api/v1/auth/check-phone | 핸드폰 번호 중복 확인 | 불필요 |
| GET | /api/v1/auth/check-nickname | 닉네임 중복 확인 | 불필요 |

### POST /api/v1/auth/signup

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

### POST /api/v1/auth/login

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

> Refresh Token은 HttpOnly Cookie로 `Set-Cookie` 헤더에 포함

### POST /api/v1/auth/refresh

**Request** — Cookie에 Refresh Token 포함 (별도 body 없음)

**Response** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### POST /api/v1/auth/logout

**Request** — Authorization 헤더에 Access Token 포함

**Response** `204 No Content`

### GET /api/v1/auth/check-email?email={email}

**Response** `200 OK`
```json
{ "available": true }
```

### GET /api/v1/auth/check-phone?phone={phone}

**Response** `200 OK`
```json
{ "available": true }
```

### GET /api/v1/auth/check-nickname?nickname={nickname}

**Response** `200 OK`
```json
{ "available": true }
```

---

## 8. 보안 요구사항

- 비밀번호 평문 전송 금지 (HTTPS 필수)
- BCrypt로 비밀번호 해싱 (cost factor 10)
- JWT Secret Key는 환경변수로 관리, 코드에 하드코딩 금지
- Refresh Token 재사용 감지 시 전체 세션 무효화
- 로그인 실패 횟수 제한으로 무차별 대입 공격 방지
- SQL Injection 방지 (JPA Parameterized Query 사용)

---

## 9. 향후 확장 계획

| 기능 | 설명 |
|------|------|
| 본인 인증 | PASS(통신사 인증) 연동, 회원가입 단계에 통합 |
| 소셜 로그인 | 카카오, 구글 OAuth2 연동 |
| 비밀번호 찾기 | 이메일로 임시 링크 발송 |
| 회원 탈퇴 | Soft Delete 처리 (status = WITHDRAWN) |
