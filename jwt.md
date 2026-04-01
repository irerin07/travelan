# Travelan JWT 설계 노트

---

## 1. Stateful vs Stateless 인증 비교

| 항목 | 세션 기반 (Stateful) | JWT 기반 (Stateless) |
|------|---------------------|---------------------|
| 인증 정보 저장 위치 | 서버 세션 | 클라이언트 토큰 |
| 매 요청 DB 조회 | 필요 (세션 → 사용자 조회) | 불필요 (토큰 서명만 검증) |
| 서버 수평 확장 | 세션 공유 필요 (Redis 등) | 독립적, 확장 용이 |
| 강제 로그아웃 | 세션 삭제로 즉시 처리 | Access Token 만료 전까지 차단 불가 |
| 보안 위협 | 세션 하이재킹 | 토큰 탈취 시 만료 전까지 악용 가능 |
| 적합 환경 | 소규모 웹 앱 | REST API, 마이크로서비스 |

### 세션 기반 인증 흐름 (Spring Security 기본)

```
POST /login (form-data: username, password)
  → UsernamePasswordAuthenticationFilter
    → UsernamePasswordAuthenticationToken 생성
    → AuthenticationManager
      → UserDetailsService.loadUserByUsername() — DB 조회
      → PasswordEncoder.matches()
    → 성공: SecurityContextHolder 저장 + 서버 세션 생성
    → 실패: SimpleUrlAuthenticationFailureHandler
```

### JWT 기반 인증 흐름

```
POST /login (JSON: email, password)
  → AuthService.login()
    → DB에서 사용자 조회 + 비밀번호 검증
    → 성공: JwtProvider → AccessToken + RefreshToken 생성

이후 요청:
  Authorization: Bearer <AccessToken>
    → JwtAuthenticationFilter
      → 서명 검증 → claims에서 userId/role 추출
      → SecurityContextHolder 설정 (DB 조회 없음)
```

---

## 2. Travelan의 선택: Stateless JWT

### UserDetailsService를 구현하지 않은 이유

Spring Security의 `UserDetailsService`는 **세션 기반**에서 매 요청마다 DB에서 사용자 정보를 조회하기 위한 인터페이스다.

Travelan은 Stateless JWT 방식을 채택했으므로 `UserDetailsService`가 불필요하다.
`JwtAuthenticationFilter`가 토큰 claims(`userId`, `role`)만으로 `SecurityContextHolder`를 설정하며, DB 조회를 완전히 생략한다.

```java
// JwtAuthenticationFilter.java
if (token != null && jwtProvider.isValid(token)) {
    Long userId = jwtProvider.getUserId(token);
    UserRole role = jwtProvider.getRole(token);
    List<GrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(userId, null, authorities);
    SecurityContextHolder.getContext().setAuthentication(authentication);
}
// DB 조회 없음
```

---

## 3. 현재 구현 구조

### 토큰 정책

| 토큰 | TTL | 저장 위치 | 용도 |
|------|-----|-----------|------|
| Access Token | 15분 | 클라이언트 메모리 | API 호출 시 Authorization 헤더 |
| Refresh Token | 30일 | HttpOnly Cookie + DB | Access Token 갱신 |

### 서명 알고리즘

- **HS256** (HMAC-SHA256)
- Secret key: 환경변수 `JWT_SECRET` (미설정 시 개발용 기본값)
- Claims: `sub` = userId, `role` = UserRole.name()

### 주요 클래스

| 클래스 | 역할 |
|--------|------|
| `JwtProperties` | `application.yaml` jwt 설정 바인딩 (`@ConfigurationProperties`) |
| `JwtProvider` | 토큰 생성(`generateAccessToken`, `generateRefreshToken`), 검증(`isValid`), 파싱(`getUserId`, `getRole`) |
| `JwtAuthenticationFilter` | `OncePerRequestFilter` — Bearer 토큰 추출 → SecurityContext 설정 |
| `AuthService` | 이메일/비밀번호/상태 검증 → 토큰 발급 → RefreshToken DB 저장 |

### 로그인 응답 구조

```
HTTP 200 OK
Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=2592000

{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

### SecurityConfig 필터 등록

`JwtAuthenticationFilter`는 `@Component`가 아니라 SecurityConfig에서 직접 인스턴스화한다.
`@Component`로 등록하면 Spring Boot가 Servlet Filter로 자동 등록하여 **이중 실행**되는 문제가 발생한다.

```java
// SecurityConfig.java
.addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                 UsernamePasswordAuthenticationFilter.class)
```

---

## 4. 현재 방식의 취약점

JWT는 서명만 유효하면 DB 조회 없이 무조건 신뢰한다. 따라서 아래 상황에서 즉각 차단이 불가능하다.

| 상황 | 현재 결과 |
|------|-----------|
| 회원 탈퇴 후 | Access Token 만료(15분)까지 API 호출 가능 |
| 계정 정지 후 | 동일 |
| 강제 로그아웃 요청 | Access Token 차단 불가 |
| 토큰 탈취 후 폐기 시도 | Access Token은 폐기 불가 |

**완화 전략**: Access Token TTL을 짧게(15분) 유지 + Refresh Token은 DB에서 검증하여 갱신 시점에 상태를 반영한다.

---

## 5. JWT Best Practice 가이드 검토 결과

### 채택한 것

| 항목 | 내용 |
|------|------|
| **Access Token TTL 15분** | 기존 1시간에서 단축. 탈취 시 blast radius 최소화 |
| **RefreshToken `revoked` 필드** | soft-delete 방식으로 replay 공격 감지 가능 (`V6__add_revoked_to_refresh_token.sql`) |
| **`@Transactional` on refresh** | Phase 4에서 구 토큰 revoke + 신규 토큰 저장을 원자적으로 처리 |

#### `revoked` 필드를 추가한 이유

DB에서 토큰을 **삭제**하면 이미 사용된 토큰인지, 처음부터 없는 토큰인지 구분이 불가능하다.
`revoked = true`로 남겨두면 → Phase 4 Rotation 시 이미 revoked된 토큰으로 재요청이 오면 **토큰 탈취로 간주**하고 해당 유저의 모든 세션을 무효화할 수 있다.

```java
// RefreshToken.java
public boolean isUsable() {
    return !revoked && !isExpired();
}

public void revoke() {
    this.revoked = true;
}
```

### 채택하지 않은 것

| 항목 | 이유 |
|------|------|
| **RS256 (비대칭 서명)** | 모놀리식 구조에서 HS256으로 충분. RS256은 마이크로서비스에서 private key 공유 없이 검증할 때 의미 있음. PEM 파일 관리 복잡도 불필요 |
| **Filter에서 UserDetailsService로 DB 조회** | 의도적으로 생략. 가이드도 "performance 중요하면 JWT claims 신뢰해도 된다"고 명시 (Checklist #5) |
| **`AuthenticationManager` 사용** | 가이드 방식은 Spring Security 파이프라인에 위임. 우리 방식(수동 검증)이 더 명시적이고 테스트하기 쉬움 |
| **`aud`/`iss` claim 검증** | 단일 서비스 구조에서 불필요 |

---

## 6. Phase 4 구현 방향

**목표**: `POST /api/v1/auth/refresh` + `POST /api/v1/auth/logout`

### Refresh Token Rotation

```
POST /api/v1/auth/refresh (Cookie: refreshToken=...)
  1. DB에서 토큰 조회 → 없으면 401
  2. isUsable() 확인 → revoked 또는 만료이면 401
  3. 이미 revoked된 토큰 → 탈취 감지 → 해당 유저 전체 토큰 revoke + 401
  4. stored.revoke() + 새 Refresh Token 저장  ← @Transactional 필수
  5. 새 Access Token + 새 Refresh Token Cookie 반환
```

### Logout

```
POST /api/v1/auth/logout (Authorization: Bearer <AccessToken>)
  1. Access Token에서 userId 추출
  2. 해당 유저의 Refresh Token 전체 revoke
  3. Set-Cookie: refreshToken=; Max-Age=0 (Cookie 삭제)
  4. 204 No Content
```
