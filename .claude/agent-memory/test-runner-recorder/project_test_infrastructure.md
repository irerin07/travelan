---
name: Test infrastructure and known failure
description: Build tool, test framework, and known recurring failure in TravelanApplicationTests for this Spring Boot project
type: project
---

Build tool: Gradle (build.gradle). Test framework: JUnit 5 + Mockito + Spring Boot Test.

130+ total tests across 11+ test classes. All tests use sliced contexts (@WebMvcTest, MockitoExtension) except TravelanApplicationTests which uses full @SpringBootTest.

`UserControllerTest` (com.irerin.travelan.user.controller.UserControllerTest): 6 tests, all covering the `withdraw` endpoint. Uses @WebMvcTest slice + Mockito. Passes cleanly as of 2026-04-07. Tests cover: unauthenticated filter block, 401 response, authenticated userId delegation to service, 404 for withdrawn user, 204 success, and RefreshToken cookie expiry header on response.

**Known recurring failure**: `com.irerin.travelan.TravelanApplicationTests#contextLoads()` fails every run because no datasource is configured for the test environment. Spring cannot determine a JDBC driver class. The Flyway + HikariCP chain all fail as a result.

**Why:** The full application context requires a real DB URL. There is no `application-test.properties` or in-memory DB override for this test class.

**How to apply:** When this failure appears, it is infrastructure-level, not a logic regression. The fix is to either add H2 test datasource config or annotate the class with `@SpringBootTest` + `@TestPropertySource` to mock the datasource, or replace with a sliced test.
