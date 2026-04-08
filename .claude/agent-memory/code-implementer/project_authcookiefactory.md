---
name: AuthCookieFactory 위치 및 WebMvcTest 주의사항
description: AuthCookieFactory는 auth.support 패키지의 @Component이며, @WebMvcTest에서 자동 로드되지 않아 @Import 필요
type: project
---

`AuthCookieFactory`는 `com.irerin.travelan.auth.support.AuthCookieFactory`에 위치한 `@Component`.
쿠키 빌드 로직(refreshTokenCookie, expiredRefreshTokenCookie)과 경로 상수(`REFRESH_TOKEN_PATH = "/api/v1/auth"`)를 한 곳에서 관리.

**Why:** 4개 엔드포인트(AuthController login/refresh/logout + UserController withdraw)의 쿠키 빌드 로직 중복 제거.
Cookie 보안 옵션 누락 방지(httpOnly/secure/sameSite 한 곳에서 관리).

**How to apply:** `@WebMvcTest`로 AuthController나 UserController를 테스트할 때
`@Import({SecurityConfig.class, AuthCookieFactory.class})` 형태로 반드시 포함시킬 것.
없으면 컨텍스트 로드 실패.
