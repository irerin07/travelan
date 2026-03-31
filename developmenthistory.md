# Travelan - 개발 일지

---

## 2026-03-27 — 프로젝트 초기 설정

### 구현 내용
- Spring Boot 4.0.5 / Java 21 기반 프로젝트 초기 생성
- QueryDSL 5.x (Jakarta) 의존성 추가
- Gradle Wrapper 9.4.0 설정

### 기술 스택 선택 이유
| 기술 | 선택 이유 |
|------|-----------|
| Spring Boot 4.x | 최신 Spring Framework 7.x 기반, Jakarta EE 완전 지원 |
| Java 21 | LTS 버전, Virtual Thread 등 최신 JVM 기능 활용 가능 |
| MySQL 8.4 | 안정적인 RDBMS, Docker로 로컬 환경 구성 |
| QueryDSL 5.x | 타입 안전한 쿼리 작성, 복잡한 동적 쿼리 대응 |

### 초기 구성 파일
- `build.gradle`: 기본 의존성 (spring-boot-starter-data-jpa, validation, web)
- `application.yaml`: 기본 앱 이름만 설정
- `compose.yaml`: Spring Boot Docker Compose 자동 생성 파일 (추후 문제 원인)

### 발생 가능한 사이드 이펙트
- Spring Boot 4.x는 3.x와 모듈 구조가 크게 달라 기존 자료/레퍼런스와 맞지 않는 경우가 많음
- `compose.yaml`이 자동 생성되어 `docker-compose.yml`보다 우선순위가 높아짐 (추후 오류 1의 원인)

---

## 2026-03-30 — 프로젝트 문서화 및 Phase 1 기반 구축

### 1. 프로젝트 문서 작성

**구현 내용**
- `spec.md`: 회원가입/로그인 기능 명세 작성
- `plan.md`: Phase 1~5 단계별 개발 계획 수립
- `tasks.md`: 74개 세부 구현 태스크 체크리스트 생성

**왜 문서를 먼저 작성했는가**
기능 구현 전 명세를 확정하여 개발 방향을 정렬하기 위함.
특히 인증 플로우(JWT, Refresh Token Rotation, 계정 잠금)는 설계 결정이 구현 복잡도에 큰 영향을 미치므로 사전 문서화가 중요하다.

**주요 설계 결정**
- 로그인 방식: 이메일/비밀번호 (소셜 로그인은 Phase 5 이후 확장)
- 이메일 인증: 생략 (추후 PASS 본인인증으로 대체 예정)
- 인증 토큰: Access Token(1시간) + Refresh Token(30일, HttpOnly Cookie)
- Refresh Token Rotation: 재사용 감지 시 전체 세션 무효화

---

### 2. Dockerfile 및 docker-compose.yml 작성

**구현 내용**
- 멀티 스테이지 빌드 Dockerfile 작성
- docker-compose.yml: MySQL 8.4 컨테이너 + 앱 컨테이너 구성

**초기 Dockerfile (문제 있는 버전)**
```dockerfile
FROM gradle:8.13-jdk21 AS build
RUN gradle bootJar --no-daemon   # ← 시스템 Gradle 사용
```

**오류 1: `Spring Boot plugin requires Gradle 8.x (8.14 or later)`**

| 항목 | 내용 |
|------|------|
| 원인 | 베이스 이미지 `gradle:8.13-jdk21`의 시스템 Gradle이 8.13이었음. Spring Boot 4.x 플러그인은 Gradle 8.14+ 또는 9.x 요구 |
| 해결 | 베이스 이미지를 `eclipse-temurin:21-jdk`로 변경, `./gradlew` (Gradle Wrapper 9.4.0) 사용 |

```dockerfile
# 수정 후
FROM eclipse-temurin:21-jdk AS build
COPY . .
RUN ./gradlew bootJar --no-daemon
```

**사이드 이펙트**
- `eclipse-temurin:21-jdk`는 Gradle이 없으므로 `./gradlew`가 없으면 빌드 불가
- Gradle Wrapper 파일(`gradlew`, `gradle/wrapper/`)을 반드시 COPY해야 함

---

### 3. Phase 1 기반 구축 구현

**구현 내용**
- 의존성 추가: `spring-boot-starter-security`, `jjwt-api 0.12.6`, `flyway-core`, `flyway-mysql`, `spring-boot-starter-actuator`
- `application.yaml`: datasource(MySQL), JPA, Flyway 설정
- 패키지 구조 생성: `auth`, `user`, `common`
- 공통 응답 구조: `ApiResponse<T>`, `ErrorResponse`
- 커스텀 예외: `DuplicateException`(409), `AuthException`(401)
- `GlobalExceptionHandler`: `@RestControllerAdvice`로 전역 예외 처리
- Flyway 마이그레이션 스크립트 3개 작성
- JPA 엔티티: `User`, `UserInterestRegion`, `RefreshToken`

**`ApiResponse<T>` 설계**
```java
// 모든 API 응답을 하나의 형식으로 통일
{
  "success": true/false,
  "data": { ... },       // 성공 시
  "error": { ... }       // 실패 시
}
```
일관된 응답 구조를 강제하여 클라이언트 파싱 로직을 단순화함.

**`users` 테이블명 선택 이유**
MySQL에서 `user`는 예약어이므로 `@Table(name = "users")`로 지정.
예약어를 테이블명으로 사용하면 일부 MySQL 버전에서 쿼리 오류 발생 가능.

**`member` → `user` 리네이밍**
초기에 `Member` 도메인으로 작성했으나 일반적인 컨벤션에 맞춰 `User`로 일괄 변경.
엔티티, SQL 스크립트, 패키지명 전체 변경.

---

### 4. 애플리케이션 기동 오류 수정

**오류 2: `Schema validation: missing table [refresh_token]`**

```
org.springframework.beans.factory.BeanCreationException
Caused by: SchemaManagementException: Schema validation: missing table [refresh_token]
```

**원인 분석**

Spring Boot Docker Compose 파일 탐색 우선순위:
```
compose.yaml        ← 1순위 (자동 생성된 파일, 문제의 원인)
compose.yml         ← 2순위
docker-compose.yaml ← 3순위
docker-compose.yml  ← 4순위 (우리가 의도한 파일)
```

프로젝트 초기화 시 자동 생성된 `compose.yaml`이 `docker-compose.yml`보다 먼저 선택되어
`mydatabase` DB + 랜덤 포트로 MySQL을 기동했다.
앱은 `travelan` DB + `3306` 포트로 연결을 시도했으나 실패 → Flyway 미실행 → 테이블 미생성 → Hibernate validate 실패.

복합 원인:
1. `compose.yaml` 파일 충돌 (주원인)
2. `ddl-auto: validate`는 Flyway가 먼저 실행된 이후에만 유효한 설정인데, 연결 자체가 실패하여 Flyway가 실행되지 않음

**해결**
1. `compose.yaml` 삭제
2. `ddl-auto: validate` → `ddl-auto: none` 변경 (Flyway가 스키마를 관리하므로 Hibernate 검증 불필요)

**사이드 이펙트**
- `ddl-auto: none`으로 변경하면 엔티티와 스키마 불일치를 런타임에서 감지하지 못함
- Flyway 마이그레이션 스크립트를 반드시 정확하게 관리해야 함

---

**오류 3: Flyway 마이그레이션 미실행 (테이블 미생성)**

오류 2 해결 후 앱이 기동되었으나 MySQL에 테이블이 생성되지 않음.

**원인**
`flyway-core`를 의존성에 추가해도 Spring Boot 4.x에서는 Flyway가 자동 실행되지 않음.
Spring Boot 4.x는 auto-configuration이 기능별로 분리된 별도 starter 모듈에 위치한다.
`flyway-core`는 라이브러리만 포함하고 auto-configuration Bean 등록이 없음.

이 사실을 발견하기 위해 fat JAR 내부의 `spring.factories` 파일을 직접 확인했다.

**1차 해결 (잘못된 접근)**
`FlywayConfig` 클래스를 수동으로 작성하여 Flyway Bean 직접 등록:
```java
@Configuration
public class FlywayConfig {
    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load();
        flyway.migrate();
        return flyway;
    }
}
```

**올바른 해결 (리팩터링)**
Spring Boot 4.0 Migration Guide 확인 후, `spring-boot-starter-flyway`를 사용하면
auto-configuration이 활성화된다는 것을 발견. `FlywayConfig` 삭제 후 starter로 교체:

```groovy
// Before
implementation 'org.flywaydb:flyway-core'

// After
implementation 'org.springframework.boot:spring-boot-starter-flyway'
implementation 'org.flywaydb:flyway-mysql'   // MySQL 방언 지원
```

**사이드 이펙트**
- `spring-boot-starter-flyway` 사용 시 `application.yaml`의 `spring.flyway.*` 설정이 자동으로 적용됨
- Flyway 설정을 코드가 아닌 yaml로 관리하므로 환경별 설정이 더 명확해짐

---

**오류 4: Docker 빌드 캐시 손상**

```
ERROR: failed to prepare extraction snapshot "extract-513782242-sDz9...":
parent snapshot sha256:163df6... does not exist: not found
```

**원인**
코드/설정 문제가 아닌 Docker 내부 빌드 캐시 데이터 불일치.
이전 빌드의 레이어 참조가 깨진 상태.

**해결**
```bash
docker builder prune -af   # 모든 빌드 캐시 강제 삭제
docker compose up --build -d
```

---

## 2026-03-31 — Phase 2 회원가입 기능 구현 및 테스트

### 구현 내용

#### 2-1. Spring Security 설정 (`SecurityConfig`)
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // CSRF 비활성화 (Stateless JWT 인증에서는 불필요)
    // Session STATELESS (서버가 세션 상태를 유지하지 않음)
    // /api/v1/auth/** 허용 (회원가입, 로그인은 인증 없이 접근 가능)
    // BCryptPasswordEncoder(cost 10) 등록
}
```

**CSRF를 비활성화한 이유**
CSRF 공격은 브라우저가 쿠키를 자동으로 전송하는 특성을 이용한다.
JWT를 Authorization 헤더로 전달하면 브라우저가 자동 전송하지 않으므로 CSRF 위협이 없다.
단, Refresh Token은 HttpOnly Cookie로 전달하므로 향후 토큰 갱신 API에 CSRF 대응 필요 여부를 재검토해야 한다.

**BCrypt cost 10을 선택한 이유**
cost가 높을수록 해싱 속도가 느려져 무차별 대입 공격에 강해진다.
cost 10은 일반적인 하드웨어에서 약 100ms 소요되어 보안과 성능의 균형점.

---

#### 2-2. `@PasswordMatch` 커스텀 유효성 어노테이션

비밀번호와 비밀번호 확인 일치 여부를 검증하는 클래스 레벨 어노테이션.

**클래스 레벨로 작성한 이유**
비밀번호 일치 검증은 두 필드(`password`, `passwordConfirm`)를 동시에 비교해야 한다.
단일 필드 어노테이션으로는 다른 필드 값에 접근할 수 없으므로 클래스 레벨 제약으로 구현.

```java
@Target(ElementType.TYPE)   // 클래스 레벨
@Constraint(validatedBy = PasswordMatchValidator.class)
public @interface PasswordMatch { ... }
```

**`addPropertyNode("passwordConfirm")`를 사용한 이유**
클래스 레벨 제약이 실패하면 기본적으로 글로벌 에러(`getGlobalErrors()`)로 처리된다.
필드 레벨 에러로 변환하여 `GlobalExceptionHandler`가 `getFieldErrors()`로 일관되게 처리할 수 있도록 `addPropertyNode`로 `passwordConfirm` 필드에 에러를 바인딩함.

---

#### 2-3. 레이어 간 DTO 분리 원칙 도입

**문제**: 컨트롤러 DTO(`SignupRequest`)를 서비스에 직접 전달하면 서비스가 웹 레이어에 의존하게 됨.

**해결**: 서비스 레이어 전용 `SignupCommand` 도입.

```
AuthController  →  SignupCommand.from(request)  →  UserService
  (웹 레이어)           (변환)                      (서비스 레이어)
```

- 웹 레이어 DTO: `auth.dto.SignupRequest` (유효성 검증 어노테이션 포함)
- 서비스 레이어 DTO: `user.dto.SignupCommand` (순수 데이터, 어노테이션 없음)
- 변환: `SignupCommand.from(request)` 정적 팩토리 메서드

**장점**
- 서비스가 웹 레이어 변경에 영향받지 않음
- 서비스 테스트 시 `@Valid` 어노테이션 없는 순수 DTO로 테스트 가능

---

#### 2-4. `UserService` 중복 검증 로직

```java
// 중복 검증 순서: 이메일 → 핸드폰 → 닉네임
// 첫 번째 중복 발견 시 즉시 DuplicateException 발생 (불필요한 DB 조회 방지)
if (userRepository.existsByEmail(command.getEmail()))    throw new DuplicateException(...);
if (userRepository.existsByPhone(command.getPhone()))    throw new DuplicateException(...);
if (userRepository.existsByNicknameIgnoreCase(...))      throw new DuplicateException(...);
```

**닉네임을 `IgnoreCase`로 비교하는 이유**
`abc`와 `ABC`는 사용자 입장에서 동일한 닉네임처럼 보일 수 있어 혼란을 방지하기 위함.

**사이드 이펙트**
- 닉네임 대소문자 무시 비교는 DB에서 `COLLATION` 설정에 따라 동작이 달라질 수 있음
- `existsByNicknameIgnoreCase`는 Spring Data JPA가 `LOWER()` 함수 또는 `ILIKE`를 사용하므로 인덱스 활용이 제한될 수 있음
- 해결: 닉네임 컬럼에 `ci` (case-insensitive) collation 적용 또는 별도 인덱스 전략 필요 (현재는 데이터 규모가 작으므로 허용)

---

#### 2-5. 중복 확인 API 응답 타입 `AvailableResponse`

`Map<String, Boolean>` 대신 전용 DTO 사용:

```java
// 금지
ResponseEntity<ApiResponse<Map<String, Boolean>>>

// 권장
ResponseEntity<ApiResponse<AvailableResponse>>
```

**이유**: `Map`은 타입 안전성이 없고, 키 오타를 컴파일 타임에 감지할 수 없음.
DTO를 사용하면 Swagger/OpenAPI 문서 자동 생성 시에도 명확한 스키마가 제공됨.

---

### TDD 도입 (RED → GREEN → REFACTOR)

Phase 2 구현 후 테스트를 추가하면서 TDD를 도입. 이후 Phase 3부터는 테스트를 먼저 작성.

#### 테스트 구성

| 테스트 클래스 | 종류 | 테스트 수 |
|--------------|------|-----------|
| `UserServiceTest` | 단위 테스트 (Mockito) | 16개 |
| `AuthControllerTest` | 슬라이스 테스트 (@WebMvcTest) | 14개 |

**`UserServiceTest` 검증 항목**
- 정상 회원가입: 응답 값, BCrypt 인코딩 확인, 관심 지역 저장 확인, null 관심 지역 허용
- 중복 예외: 이메일/핸드폰/닉네임 중복 시 `DuplicateException` + `save()` 미호출 확인
- 가용성 확인: isEmailAvailable, isPhoneAvailable, isNicknameAvailable

**`AuthControllerTest` 검증 항목**
- 정상 요청 → 201 Created + 응답 body 확인
- 유효성 실패 케이스: 이메일 공백/형식 오류, 비밀번호 7자 이하/공백 포함/불일치, 이름 공백, 핸드폰 형식 오류, 닉네임 11자 이상/공백 포함, 관심 지역 6개 이상 → 400 Bad Request
- 중복 이메일 → 409 Conflict
- check-email/phone/nickname API 정상 응답

---

### 오류 5: @WebMvcTest 컴파일 오류 (Spring Boot 4.x 모듈 분리)

```
package org.springframework.boot.test.autoconfigure.web.servlet does not exist
```

**원인**
Spring Boot 4.x에서 테스트 슬라이스도 별도 모듈로 분리됨.
`@WebMvcTest`가 `spring-boot-test-autoconfigure`에서 제거되어 `spring-boot-starter-webmvc-test`로 이동.
또한 `spring-security-test`를 사용하면 Security 필터 호출 순서가 달라져 인증 필터 테스트가 올바르게 동작하지 않음.

**해결**
```groovy
// Before
testImplementation 'org.springframework.security:spring-security-test'

// After
testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
```

```java
// Before (Spring Boot 3.x)
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

// After (Spring Boot 4.x)
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
```

**사이드 이펙트**
- `spring-boot-starter-webmvc-test`는 `spring-security-test`를 이미 포함하므로 별도 추가 불필요
- Spring Boot 4.x로 업그레이드할 때 기존의 모든 `@WebMvcTest`, `@DataJpaTest` 등의 슬라이스 테스트가 동일하게 영향받음

---

### 오류 6: 테스트 assertion 오류

**오류 1 — `$.error.fieldErrors` 경로 없음**

`@PasswordMatch` 불일치 테스트에서 `PathNotFoundException` 발생.

원인: `ErrorResponse`의 실제 필드명이 `fieldErrors`가 아닌 `errors`였음.

```java
// 잘못된 경로
.andExpect(jsonPath("$.error.fieldErrors[0].field").value("passwordConfirm"));

// 올바른 경로
.andExpect(jsonPath("$.error.errors[0].field").value("passwordConfirm"));
```

**오류 2 — 닉네임 11자 테스트 문자열이 실제로 10자**

```java
// "열한글자닉네임입니다" = 10글자 → @Size(max=10) 통과 → validation 성공 → 400 아닌 201 반환
validSignupJson().replace("\"여행자\"", "\"열한글자닉네임입니다\"");   // 잘못됨

// 수정: 실제 11글자
validSignupJson().replace("\"여행자\"", "\"닉네임이열한글자입니다\"");  // 11글자
```

---

### 최종 테스트 결과

```
30 tests completed, 0 failed
BUILD SUCCESSFUL
```
