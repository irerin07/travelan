# SecurityContextHolder → @AuthenticationPrincipal 리팩토링

## 배경
컨트롤러에서 인증된 사용자 ID를 추출하기 위해 다음 패턴을 사용하고 있었음:

```java
Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
```

### 문제점
1. **Null-safety 경고**: `Authentication.getPrincipal()`의 반환 타입이 `Object`(nullable)라 정적 분석 도구가 NPE 경고를 출력
2. **명시적 캐스팅**: `(Long)` 다운캐스팅으로 타입 안정성을 컴파일러가 보장하지 못함
3. **테스트 가독성 저하**: `SecurityContextHolder`는 ThreadLocal 기반이라 단위 테스트에서 직접 조작이 번거로움
4. **Spring Security 권장 패턴 미준수**: Spring은 `@AuthenticationPrincipal` 어노테이션을 표준 방식으로 권장

---

## 해결책: `@AuthenticationPrincipal` 어노테이션

```java
@DeleteMapping("/me")
public ResponseEntity<Void> withdraw(
    @AuthenticationPrincipal Long userId,
    HttpServletResponse httpResponse
) {
    userService.withdraw(userId);
    ...
}
```

### 동작 원리
1. `JwtAuthenticationFilter`가 토큰 검증 후 `UsernamePasswordAuthenticationToken(userId, ...)`을 생성하며 principal에 `Long` 타입 `userId`를 세팅
2. Spring MVC의 `AuthenticationPrincipalArgumentResolver`가 컨트롤러 메서드 호출 시 SecurityContext에서 principal을 꺼내 자동 주입
3. principal 타입이 파라미터 타입과 일치하면 캐스팅 없이 바로 매핑

### Null이 되지 않는 이유
- `SecurityConfig`의 `anyRequest().authenticated()` 정책으로 미인증 요청은 컨트롤러 진입 전 401로 차단됨
- 따라서 컨트롤러 메서드가 호출되는 시점에는 항상 유효한 principal이 존재
- `@AuthenticationPrincipal`은 `ArgumentResolver` 단계에서 처리되므로 정적 분석 경고가 발생하지 않음

---

## 변경 파일

### 1. `src/main/java/com/irerin/travelan/user/controller/UserController.java`
**Before**
```java
import org.springframework.security.core.context.SecurityContextHolder;

@DeleteMapping("/me")
public ResponseEntity<Void> withdraw(HttpServletResponse httpResponse) {
    Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    userService.withdraw(userId);
    ...
}
```

**After**
```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@DeleteMapping("/me")
public ResponseEntity<Void> withdraw(
    @AuthenticationPrincipal Long userId,
    HttpServletResponse httpResponse
) {
    userService.withdraw(userId);
    ...
}
```

### 2. `src/main/java/com/irerin/travelan/auth/controller/AuthController.java`
**Before**
```java
import org.springframework.security.core.context.SecurityContextHolder;

@PostMapping("/logout")
public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
    Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    authService.logout(userId);
    ...
}
```

**After**
```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@PostMapping("/logout")
public ResponseEntity<Void> logout(
    @AuthenticationPrincipal Long userId,
    HttpServletResponse httpResponse
) {
    authService.logout(userId);
    ...
}
```

---

## 변경하지 않은 곳

### `JwtAuthenticationFilter.java`
```java
SecurityContextHolder.getContext().setAuthentication(authentication);
```

이 호출은 **인증 정보를 컨텍스트에 쓰는 쪽**으로, `@AuthenticationPrincipal`은 이를 읽기 위한 메커니즘이므로 대체 불가.
필터가 이 호출을 수행해야만 컨트롤러의 `@AuthenticationPrincipal`이 동작한다. 그대로 유지.

---

## 검토한 대안

| 방식 | 채택 여부 | 사유 |
|---|---|---|
| `@AuthenticationPrincipal Long userId` | ✅ 채택 | 표준 Spring 패턴, 캐스팅·null 경고 모두 제거 |
| `Authentication` 파라미터 주입 후 캐스팅 | ❌ | principal 캐스팅·null 경고가 동일하게 발생 |
| 커스텀 `UserPrincipal` 클래스 도입 | ❌ | 현재 principal이 `Long` 하나라 과도한 추상화 |
| `Optional.ofNullable(principal).orElseThrow()` | ❌ | 도달 불가능한 방어 코드 (dead branch) |

---

## 검증
- `./gradlew test` 전체 GREEN
- `AuthControllerTest.logout_*`, `UserControllerTest.withdraw_*` 단위 테스트 모두 통과
- 정적 분석 NPE 경고 해소

## 참고
- Spring Security Reference — [@AuthenticationPrincipal](https://docs.spring.io/spring-security/reference/servlet/integrations/mvc.html#mvc-authentication-principal)
