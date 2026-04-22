# DataSource 분리 과정에서 발생한 이슈

## 이슈 1: Docker 환경 DB 연결 실패 (Communications link failure)

### 증상
```
Unable to obtain connection from database: Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago.
```

### 원인
`docker-compose.yml`의 환경변수가 변경된 yaml 구조와 매핑되지 않았다.

DataSource 구조를 `spring.datasource.url`에서 `spring.datasource.write.url` / `spring.datasource.read.url`로 변경했으나, `docker-compose.yml`의 환경변수는 이전 구조(`SPRING_DATASOURCE_URL`)를 그대로 사용하고 있었다.

컨테이너 내부에서 환경변수 오버라이드가 적용되지 않아 `application.yaml`의 `localhost:3306`으로 연결을 시도했고, 이는 앱 컨테이너 자신을 가리키므로 연결에 실패했다.

### 해결
환경변수를 새 구조에 맞게 변경했다.
```yaml
# 변경 전
SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/travelan

# 변경 후
SPRING_DATASOURCE_WRITE_URL: jdbc:mysql://mysql:3306/travelan
SPRING_DATASOURCE_READ_URL: jdbc:mysql://mysql:3306/travelan
```

---

## 이슈 2: MySQL 공개키 인증 실패 (Public Key Retrieval is not allowed)

### 증상
```
Unable to obtain connection from database: Public Key Retrieval is not allowed
SQL State: 08001
```

### 원인
이슈 1을 수정하면서 `application.yaml`(로컬용)의 JDBC URL을 Docker 환경변수에 그대로 복사했다. 이전 `docker-compose.yml`은 `jdbc:mysql://mysql:3306/travelan`(파라미터 없음)만 사용하고 있었으나, 복사한 URL에는 `useSSL=false`가 포함되어 있었다.

MySQL 8.4의 기본 인증 플러그인(`caching_sha2_password`)은 SSL이 비활성화된 상태에서 공개키를 통한 비밀번호 교환이 필요한데, JDBC 드라이버는 기본적으로 공개키 조회를 허용하지 않는다.

이전에는 파라미터 없이 연결하여 SSL이 기본 활성화 상태였으므로 암호화 채널을 통해 인증이 정상 처리되었다.

### 해결
불필요하게 추가된 파라미터를 제거하고 이전과 동일한 최소 URL로 복원했다.
```yaml
# 불필요한 파라미터 제거
SPRING_DATASOURCE_WRITE_URL: jdbc:mysql://mysql:3306/travelan
SPRING_DATASOURCE_READ_URL: jdbc:mysql://mysql:3306/travelan
```

---

## 이슈 3: HikariDataSource 패키지명 오류 (컴파일 실패)

### 증상
```
error: package com.zaxxus.hikari does not exist
import com.zaxxus.hikari.HikariDataSource;
```

### 원인
`DataSourceConfig.java`에서 `HikariDataSource`의 패키지명을 `com.zaxxus.hikari`로 잘못 작성했다. 정확한 패키지는 `com.zaxxer.hikari`이다.

### 해결
```java
// 변경 전
import com.zaxxus.hikari.HikariDataSource;

// 변경 후
import com.zaxxer.hikari.HikariDataSource;
```

---

## 이슈 4: 라우팅 로그 미출력으로 인한 동작 확인 불가

### 증상
첫 번째 확인 시 `READ_ONLY` 로그만 보이고 `READ_WRITE` 로그가 보이지 않아 라우팅이 정상 동작하지 않는 것처럼 보였다.

### 원인
`RoutingDataSource`의 로그 레벨이 `DEBUG`였으나, `application-dev.yaml`에 해당 로거의 레벨 설정이 누락되어 있었다. 설정을 추가했으나 Hibernate의 `show-sql` 출력과 라우팅 로그가 서로 다른 스레드에서 인터리빙되면서 가독성이 떨어졌다.

### 해결
로그 레벨을 `DEBUG`에서 `INFO`로 변경하여 별도 설정 없이도 항상 출력되도록 했다.
```java
// 변경 전
log.debug("Routing to datasource: {}", type);

// 변경 후
log.info(">>> Routing to datasource: {}", type);
```

INFO 레벨 로그로 전체 확인한 결과, `READ_WRITE`와 `READ_ONLY` 모두 정상 라우팅되고 있음을 확인했다.
