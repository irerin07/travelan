# Test Results — 2026-04-03

## Summary
- **Total tests run**: 130
- **Passed**: 129
- **Failed**: 1
- **Skipped/Ignored**: 0
- **Overall status**: FAILURES DETECTED

## Failed Tests

### 1. com.irerin.travelan.TravelanApplicationTests#contextLoads()
- **Class**: `com.irerin.travelan.TravelanApplicationTests`
- **Method**: `contextLoads()`
- **Failure type**: `java.lang.IllegalStateException` — Failed to load ApplicationContext
- **Message**: Failed to determine a suitable driver class
- **Relevant stack trace**:
  ```
  java.lang.IllegalStateException: Failed to load ApplicationContext
      at org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.lambda$loadContext$0(DefaultCacheAwareContextLoaderDelegate.java:195)
  Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' ...
      Failed to initialize dependency 'flyway' of LoadTimeWeaverAware bean 'entityManagerFactory'
  Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'flyway' ...
      Failed to instantiate [org.flywaydb.core.Flyway]: Factory method 'flyway' threw exception with message:
      Error creating bean with name 'dataSource' ...
  Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [com.zaxxer.hikari.HikariDataSource]:
      Factory method 'dataSource' threw exception with message: Failed to determine a suitable driver class
  Caused by: org.springframework.boot.jdbc.autoconfigure.DataSourceProperties$DataSourceBeanCreationException:
      Failed to determine a suitable driver class
      at org.springframework.boot.jdbc.autoconfigure.DataSourceProperties.determineDriverClassName(DataSourceProperties.java:179)
      at org.springframework.boot.jdbc.autoconfigure.PropertiesJdbcConnectionDetails.getDriverClassName(PropertiesJdbcConnectionDetails.java:51)
      at org.springframework.boot.jdbc.autoconfigure.DataSourceConfiguration$Hikari.dataSource(DataSourceConfiguration.java:128)
  ```
- **Possible cause**: `TravelanApplicationTests` uses `@SpringBootTest` which loads the full application context including the real datasource. No `spring.datasource.url` (or equivalent) is configured for the test environment, so Spring cannot resolve a JDBC driver class. The test likely needs either a `@DataJpaTest`/`@WebMvcTest` slice annotation to avoid loading the datasource, an `application-test.properties` pointing at an in-memory DB (e.g., H2), or a `@MockBean` / `@TestPropertySource` that overrides the datasource properties. All other 129 tests pass because they use sliced test contexts (`@WebMvcTest`, `@ExtendWith(MockitoExtension.class)`, etc.) that do not start the full datasource.
