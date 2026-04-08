# Test Results — 2026-04-07

## Summary
- **Total tests run**: 6
- **Passed**: 6
- **Failed**: 0
- **Skipped/Ignored**: 0
- **Overall status**: ALL PASSED

## Scope
Class: `com.irerin.travelan.user.controller.UserControllerTest`
Command: `./gradlew test --tests "com.irerin.travelan.user.controller.UserControllerTest" --rerun-tasks`

## Failed Tests
No failures.

## Test Cases (all passed)

| # | Method | Time (s) |
|---|--------|----------|
| 1 | `withdraw_미인증_요청은_필터에서_차단되어_서비스가_호출되지_않는다()` | 0.122 |
| 2 | `withdraw_미인증_401_반환()` | 0.047 |
| 3 | `withdraw_인증된_요청은_서비스에_non_null_userId를_전달한다()` | 0.155 |
| 4 | `withdraw_이미_탈퇴된_회원_404_반환()` | 0.020 |
| 5 | `withdraw_인증된_사용자_204_반환()` | 0.006 |
| 6 | `withdraw_응답에_RefreshToken_Cookie_만료_헤더_포함()` | 0.020 |

## Notes
- Tests run via `@WebMvcTest` slice with Mockito inline-mock-maker (self-attaching agent warning present — non-fatal, standard JDK 21 behavior).
- All tests cover the user withdrawal (`withdraw`) endpoint across authentication, authorization, service delegation, and response header scenarios.
