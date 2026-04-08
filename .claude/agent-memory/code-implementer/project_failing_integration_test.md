---
name: TravelanApplicationTests contextLoads 실패
description: @SpringBootTest 전체 컨텍스트 로드 테스트는 MySQL 연결 없이 항상 실패하므로 테스트 결과 해석 시 제외
type: project
---

`TravelanApplicationTests.contextLoads()`는 `@SpringBootTest`로 전체 Spring 컨텍스트를 로드하는 통합 테스트이다.

**Why:** 이 테스트는 실제 MySQL(localhost:3306/travelan)에 연결을 시도하므로, DB가 없는 개발/CI 환경에서 항상 `DataSourceBeanCreationException`으로 실패한다. 이는 우리 코드 변경과 무관한 인프라 이슈이다.

**How to apply:** `./gradlew test` 전체 실행 시 이 1건의 실패는 무시해도 된다. 나머지 113개 테스트가 모두 통과하면 코드 변경이 올바른 것이다. 단위/슬라이스 테스트만 실행할 때는 `--tests "com.irerin.travelan.auth.*" --tests "com.irerin.travelan.user.*" --tests "com.irerin.travelan.common.*"` 패턴을 사용한다.
