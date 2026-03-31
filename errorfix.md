# Travelan - 오류 해결 기록

---

## 오류 1: Schema validation: missing table [refresh_token]

### 오류 메시지
```
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory'
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: Schema validation: missing table [refresh_token]
```

### 원인
두 가지 문제가 복합적으로 발생.

**원인 1 - `compose.yaml` 파일 충돌 (주원인)**

Spring Boot Docker Compose는 다음 우선순위로 파일을 탐색한다.
```
compose.yaml        ← 최우선 (문제의 파일)
compose.yml
docker-compose.yaml
docker-compose.yml  ← 의도한 파일
```
프로젝트에 `compose.yaml`(자동 생성된 파일)이 존재했고, 이 파일이 먼저 선택되어 잘못된 DB(`mydatabase`, 랜덤 포트)로 MySQL을 기동했다.
우리 앱은 `travelan` DB + `3306` 포트로 연결을 시도했지만 연결에 실패하여 Flyway가 실행되지 않았고, 테이블이 생성되지 않아 Hibernate 검증 단계에서 실패했다.

**원인 2 - `ddl-auto: validate` 부적절**

Flyway가 스키마를 관리하는 상황에서 `ddl-auto: validate`를 사용하면 Flyway와 JPA 초기화 순서 문제가 발생할 수 있다.

### 해결
1. `compose.yaml` 파일 삭제
2. `application.yaml`에서 `ddl-auto: validate` → `ddl-auto: none` 변경

```yaml
# Before
spring:
  jpa:
    hibernate:
      ddl-auto: validate

# After
spring:
  jpa:
    hibernate:
      ddl-auto: none   # Flyway가 스키마를 관리하므로 Hibernate 검증 불필요
```

---

## 오류 2: Spring Boot plugin requires Gradle 8.x (8.14 or later)

### 오류 메시지
```
> Failed to apply plugin 'org.springframework.boot'.
   > Spring Boot plugin requires Gradle 8.x (8.14 or later) or 9.x. The current version is Gradle 8.13
```

### 원인
`Dockerfile`에서 시스템 Gradle(`gradle` 명령어)을 직접 호출했는데, 베이스 이미지(`gradle:8.13-jdk21`)의 Gradle 버전이 8.13이었다.
Spring Boot 4.x 플러그인은 Gradle 8.14 이상 또는 9.x를 요구하므로 버전 불일치로 빌드 실패.

```dockerfile
# 문제
FROM gradle:8.13-jdk21 AS build
RUN gradle bootJar --no-daemon    # 시스템 Gradle 8.13 사용
```

프로젝트에는 Gradle Wrapper(9.4.0)가 포함되어 있어 `./gradlew`를 사용하면 문제가 없다.

### 해결
베이스 이미지를 JDK 이미지로 변경하고 `./gradlew`(Gradle Wrapper) 사용.

```dockerfile
# After
FROM eclipse-temurin:21-jdk AS build
RUN ./gradlew bootJar --no-daemon  # Gradle Wrapper 9.4.0 사용
```

---

## 오류 3: Flyway 마이그레이션 미실행 (테이블 미생성)

### 증상
앱이 정상 기동되었지만 MySQL에 테이블이 하나도 생성되지 않음.
애플리케이션 로그에 Flyway 관련 출력이 전혀 없음.

### 원인
`build.gradle`에 `flyway-core`만 추가하면 Flyway 라이브러리만 포함될 뿐, Spring Boot auto-configuration이 활성화되지 않는다.
Spring Boot 4.x에서는 각 기능의 auto-configuration이 별도 starter 모듈로 분리되어 있다.

### 해결
`flyway-core` 대신 `spring-boot-starter-flyway`를 사용하면 auto-configuration이 활성화되어 Flyway가 자동으로 실행된다.

```groovy
// Before (auto-configuration 미포함)
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-mysql'

// After (auto-configuration 포함)
implementation 'org.springframework.boot:spring-boot-starter-flyway'
implementation 'org.flywaydb:flyway-mysql'   // MySQL 방언 지원을 위해 유지
```

참고: [Spring Boot 4.0 Migration Guide - Module Dependencies](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#module-dependencies)

---

## 오류 4: Docker 빌드 캐시 손상

### 오류 메시지
```
ERROR: failed to prepare extraction snapshot "extract-513782242-sDz9 ...":
parent snapshot sha256:163df6... does not exist: not found
```

### 원인
Docker 빌드 캐시가 손상되어 이전 빌드의 레이어를 참조할 수 없는 상태.
코드나 설정 문제가 아닌 Docker 내부 캐시 데이터 불일치로 발생.

### 해결
Docker 빌드 캐시 전체 초기화 후 재빌드.

```bash
docker builder prune -af   # 모든 빌드 캐시 강제 삭제
docker compose up --build -d
```

---

## 오류 5: @WebMvcTest 컴파일 오류 및 Security 필터 순서 문제

### 오류 메시지
```
package org.springframework.boot.test.autoconfigure.web.servlet does not exist
```

### 원인
Spring Boot 4.x에서 `@WebMvcTest`가 `spring-boot-test-autoconfigure`에서 분리되어 별도 모듈로 이동.
또한 `spring-security-test`를 그대로 사용하면 Security 필터 호출 순서가 달라져 인증 필터 테스트가 실패함.

### 해결
`build.gradle` 테스트 의존성 변경:

```groovy
// Before
testImplementation 'org.springframework.security:spring-security-test'

// After
testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
```

`@WebMvcTest` import 경로 변경:

```java
// Before (Spring Boot 3.x)
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

// After (Spring Boot 4.x)
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
```
