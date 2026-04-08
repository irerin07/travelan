# Memory Index

- [API 응답에 Map 사용 금지](feedback_no_map_response.md) — 컨트롤러 응답으로 Map 대신 항상 전용 DTO 클래스 사용
- [TDD 개발 방식 필수 적용](feedback_tdd.md) — 기능 구현 시 RED→GREEN→REFACTOR 사이클 준수
- [TravelanApplicationTests contextLoads 실패](project_failing_integration_test.md) — @SpringBootTest 전체 컨텍스트 로드 테스트는 MySQL 연결 없이 항상 실패
- [AuthCookieFactory WebMvcTest 주의사항](project_authcookiefactory.md) — auth.support.AuthCookieFactory는 @WebMvcTest에서 @Import 필요
- [WebMvcTest + SecurityConfig 주의사항](project_webmvctest_securityconfig.md) — SecurityConfig @Import 시 JwtProvider + UserRepository 모두 @MockitoBean 필요, 인증 테스트엔 userRepository stub 필수
