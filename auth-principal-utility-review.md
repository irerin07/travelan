# `@AuthenticationPrincipal` 유틸리티 클래스 제안 리뷰

## 배경

`UserController.withdraw()`에서 `@AuthenticationPrincipal Long userId`를 사용하고 있는데, 매번 null 체크를 반복해야 하는 것이 번거로워 **`@AuthenticationPrincipal`과 동일한 역할 + null 체크**를 수행하는 **정적 유틸리티 클래스**를 만드는 방안을 검토함.

---

## 📋 Plan Summary

`SecurityContextHolder`에서 `userId`를 추출하고 null이면 예외를 던지는 static utility를 만들어, 컨트롤러마다 반복되는 null 체크를 DRY하게 제거하자는 계획.

---

## 🎯 Verdict: ❌ Request Changes

**핵심 결론: null 체크 자체가 불필요하다.**

- `SecurityConfig`에 이미 `.anyRequest().authenticated()`가 설정되어 있어, 인증되지 않은 요청은 필터 체인 단계에서 401(`authenticationEntryPoint`)로 거부됨 → 컨트롤러에 도달하지 않음.
- `JwtAuthenticationFilter`는 항상 `Long` principal을 세팅하므로, 보호된 엔드포인트에서는 `userId`가 null이 될 수 없음.
- 이미 `AuthController.logout()`도 `@AuthenticationPrincipal Long userId`를 null 체크 없이 사용 중이며 문제가 없음.

---

## 👎 Concerns

### 🔴 Blockers

**1. 존재하지 않는 문제를 해결하는 것 (phantom problem).**
필터 체인이 이미 null 케이스를 차단하고 있음. 방어 코드를 추가하면 "런타임에 null이 올 수 있다"는 오해를 후속 개발자에게 남김.

**2. 정적 유틸리티는 테스트 용이성을 해침.**
`SecurityContextHolder`는 static thread-local global이라, 유틸리티를 호출하는 컨트롤러를 테스트하려면 매 테스트마다 SecurityContext를 세팅해야 함. 현재의 `@AuthenticationPrincipal Long userId` 시그니처는 `withdraw(42L, mockResponse)`처럼 단순 호출로 테스트 가능.

### 🟡 Major

**3. 숨은 의존성 / SRP 위반.**
컨트롤러가 HTTP 처리 + 전역 레지스트리에서 자기 principal 꺼내기라는 두 가지 책임을 가지게 됨. 시그니처만 봐서는 의존성을 알 수 없음.

**4. 일관성 문제.**
일부는 `@AuthenticationPrincipal`, 일부는 유틸리티를 쓰게 되어 코드베이스가 읽기 어려워짐.

---

## ❓ 사용자 반박과 재검토

> `Authentication.getPrincipal()`의 반환 타입이 `@Nullable Object`이므로 `@AuthenticationPrincipal Long userId`는 **타입 시스템상 nullable**이고, IDE/정적 분석이 경고한다.

**타입 레벨에서는 맞는 지적.** 하지만:

1. **런타임에는 현재 `SecurityConfig` 아래에서 null이 될 수 없음.**
   `.anyRequest().authenticated()`는 `Authentication == null`과 `AnonymousAuthenticationToken`을 모두 거부함. 즉, `withdraw()`에 도달하는 요청은 반드시 `JwtAuthenticationFilter`가 non-null `Long`을 principal로 세팅한 요청임. 인터페이스의 `@Nullable`은 **모든 `Authentication` 구현체**를 위한 일반 계약일 뿐, 보호된 엔드포인트의 실제 런타임 케이스는 아님.

2. **IDE 경고를 해결하는 올바른 방법도 정적 유틸리티가 아님.**
   `AuthUtils.getUserId()` 같은 정적 메서드는:
   - 컨트롤러 시그니처에서 의존성을 숨김
   - 테스트마다 thread-local을 세팅하게 만듦
   - null 체크를 단지 **이동**시킬 뿐, 호출부의 타입을 non-null로 만들어주지도 않음

---

## ✨ Best Practices & Recommendations

### Option 1: 현행 유지 (현재 코드베이스에 권장)

null 문제는 존재하지 않음. `SecurityConfig`가 이미 필터 단계에서 보장하고, 401 응답도 `ErrorResponse.of("UNAUTHORIZED", ...)`로 깔끔하게 포맷되고 있음. `@AuthenticationPrincipal Long userId` 시그니처를 그대로 두는 것이 가장 명확하고 테스트 친화적.

### Option 2 (권장): 커스텀 `HandlerMethodArgumentResolver` + `@CurrentUserId`

보호된 엔드포인트가 늘어나 중앙집중이 필요해지면, Spring MVC의 **정석 패턴**인 ArgumentResolver를 사용할 것.

```java
// Annotation
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {}
```

```java
// Resolver
public class CurrentUserIdResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
            && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ...,
                                  NativeWebRequest webRequest, ...) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            // 실제로는 도달 불가능하지만 @Nullable 계약은 존중
            throw new AuthException("인증이 필요합니다");
        }
        return id; // 이 지점 이후로는 non-null 보장
    }
}
```

```java
// 컨트롤러 사용
public ResponseEntity<Void> withdraw(@CurrentUserId Long userId, HttpServletResponse response)
```

**정적 유틸리티 대비 이점:**
- `userId`가 **명시적인 메서드 파라미터**로 유지됨 → 단위 테스트에서 그냥 `Long`을 넘기면 됨
- null/`instanceof` 체크가 **정확히 한 곳**에 존재
- `AuthException`이 `GlobalExceptionHandler`의 기존 예외 처리 체인과 자연스럽게 연동
- 비즈니스 로직에서 static global에 대한 의존성이 없음
- 컨트롤러 시그니처가 non-null `Long`을 받으므로 **IDE nullability 경고도 해결됨**

### Option 3: SpEL 메타 어노테이션 — 불필요
이미 principal이 `Long` 그 자체이므로 `@AuthenticationPrincipal(expression = ...)`은 이점이 없음.

### Option 4: 통합 테스트 추가
"누가 실수로 Security 규칙을 지우면?"이 걱정이라면, JWT 없이 `DELETE /api/v1/users/me` 요청 시 401을 반환하는지 검증하는 통합 테스트를 추가하는 것이 컨트롤러에 null 체크를 넣는 것보다 올바른 해법.

---

## 📐 프로젝트 컨벤션 정합성

CLAUDE.md의 하드 룰(Map 응답 금지, 레이어 간 DTO 분리, `new` 직접 사용 금지)과는 충돌하지 않음. 다만 프로젝트의 암묵적 철학 — 의존성을 명시적으로 드러내고 TDD 친화적인 코드 — 과는 정적 유틸리티가 충돌함. Static global은 TDD 셋업을 무겁게 만들기 때문.

---

## 요약

| 항목 | 정적 유틸리티 | `@AuthenticationPrincipal` (현행) | `@CurrentUserId` + Resolver |
|---|---|---|---|
| null 체크 중앙화 | ✅ | ❌ (하지만 불필요) | ✅ |
| 컨트롤러 시그니처 명시성 | ❌ | ✅ | ✅ |
| 단위 테스트 용이성 | ❌ | ✅ | ✅ |
| IDE nullability 경고 해결 | ❌ (이동만) | ❌ | ✅ |
| Spring 관용 패턴 | ❌ | ✅ | ✅✅ |
| 현재 필요성 | — | **현재 최적** | 엔드포인트 증가 시 이전 |
