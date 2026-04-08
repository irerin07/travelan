---
name: WebMvcTest + SecurityConfig 주의사항
description: SecurityConfig를 @Import한 @WebMvcTest 슬라이스 테스트에서 SecurityConfig가 요구하는 모든 Bean을 @MockitoBean으로 등록해야 컨텍스트가 로드됨
type: project
---

SecurityConfig.securityFilterChain 메서드는 JwtProvider와 UserRepository를 파라미터로 받는다.
@WebMvcTest + @Import(SecurityConfig.class)를 사용하는 테스트 클래스는 반드시 아래 두 Bean을 @MockitoBean으로 등록해야 한다:

```java
@MockitoBean JwtProvider jwtProvider;
@MockitoBean UserRepository userRepository;
```

**Why:** SecurityConfig Bean 메서드가 스프링 DI를 통해 JwtProvider와 UserRepository를 받는데, @WebMvcTest 슬라이스는 JPA 리포지토리를 로드하지 않아 UserRepository Bean이 없으면 컨텍스트 로드에 실패한다.

**How to apply:** 새로운 @WebMvcTest 테스트를 만들 때마다 SecurityConfig를 @Import하면 위 두 Bean을 @MockitoBean으로 추가할 것.
또한 유효한 토큰 인증이 필요한 테스트(jwtProvider stub이 있는 테스트)에서는 userRepository.findById(userId)가 ACTIVE 사용자를 반환하도록 stub해야 한다 — 그렇지 않으면 JwtAuthenticationFilter가 인증을 설정하지 않아 401이 반환된다.
