# DataSource Read/Write 분리

## 요청 사항
`@Transactional(readOnly = true)`일 때 read 전용 DataSource를, `@Transactional(readOnly = false)`일 때 read/write용 DataSource를 사용하도록 DataSource를 두 개로 분리한다.

## 구현 내용

### 아키텍처

```
@Transactional(readOnly=true)  ──┐
                                 ├──  TransactionSynchronizationManager.isCurrentTransactionReadOnly()
@Transactional(readOnly=false) ──┘
                                          │
                                  RoutingDataSource.determineCurrentLookupKey()
                                          │
                                   ┌──────┴──────┐
                                   │             │
                               READ_ONLY     READ_WRITE
                              DataSource     DataSource
                                   │             │
                               (replica)     (primary)
```

### 변경 파일

| 파일 | 경로 | 상태 |
|------|------|------|
| DataSourceType | `common/config/DataSourceType.java` | 신규 |
| RoutingDataSource | `common/config/RoutingDataSource.java` | 신규 |
| DataSourceConfig | `common/config/DataSourceConfig.java` | 신규 |
| application.yaml | `src/main/resources/application.yaml` | 수정 |

### 각 파일의 역할

**DataSourceType** — `READ_WRITE`, `READ_ONLY` 두 값을 가진 enum. 라우팅 키로 사용된다.

**RoutingDataSource** — `AbstractRoutingDataSource`를 상속하며, `TransactionSynchronizationManager.isCurrentTransactionReadOnly()`의 반환값에 따라 라우팅 키를 결정한다.

**DataSourceConfig** — 핵심 설정 클래스.
- `writeDataSource` / `readDataSource` 두 개의 커넥션 풀 생성
- `RoutingDataSource`에 두 DataSource를 매핑
- `LazyConnectionDataSourceProxy`로 감싸서 `@Primary` 빈으로 등록
- `@FlywayDataSource`를 writeDataSource에 부여하여 마이그레이션은 write DS만 사용
- `@Profile("!test")`로 테스트 프로필에서 제외

**application.yaml** — 기존 flat `spring.datasource.*`를 `spring.datasource.write.*`와 `spring.datasource.read.*`로 분리. 현재 동일 호스트를 가리키며, read replica 도입 시 URL만 변경하면 된다.

### 핵심 설계 결정

**LazyConnectionDataSourceProxy 사용** — 이것 없이는 커넥션이 `@Transactional`의 readOnly 속성 설정 전에 획득되어, 모든 트래픽이 write DS로 라우팅된다. 이 프록시가 커넥션 획득을 첫 SQL 실행 시점까지 지연시킨다.

**별도 ThreadLocal(DataSourceContextHolder) 미사용** — 모든 서비스가 이미 `@Transactional(readOnly = true)`를 클래스 레벨에, `@Transactional`을 write 메서드에 일관되게 적용하고 있어 Spring의 `TransactionSynchronizationManager`만으로 충분하다.

## 트레이드오프

### 얻은 것

- **Read Replica 즉시 적용 가능** — `spring.datasource.read.url`만 변경하면 읽기 트래픽을 replica로 분산할 수 있다
- **Write DB 부하 감소** — 읽기 전용 쿼리가 별도 커넥션 풀을 사용하므로, write 커넥션 풀의 고갈 위험이 줄어든다
- **기존 코드 무변경** — 서비스 레이어의 `@Transactional` 어노테이션이 그대로 라우팅 기준이 되므로 비즈니스 코드 수정이 없다

### 잃은 것

- **커넥션 풀 2배** — 동일 DB를 가리키더라도 HikariCP 풀이 두 개 생성된다. 각 풀의 기본 max size(10)를 합산하면 DB 커넥션 소비가 기존 대비 2배가 된다. DB 서버의 `max_connections` 설정을 확인해야 한다
- **설정 복잡도 증가** — DataSource 관련 설정이 auto-config 한 줄에서 커스텀 Configuration 클래스로 바뀌었다. HikariCP 튜닝(pool size, timeout 등)을 할 때 write/read 각각 별도로 설정해야 한다
- **디버깅 난이도 증가** — 쿼리가 어느 DataSource로 라우팅되었는지 로그만으로는 즉시 파악이 어렵다. 문제 발생 시 RoutingDataSource에 로그를 추가하거나 HikariCP 풀 이름을 구분하는 추가 설정이 필요하다
- **Replication Lag 감수** — 실제 replica를 사용할 경우, write 직후 read가 최신 데이터를 반환하지 않을 수 있다. `PostService.get()`처럼 readOnly 트랜잭션 내에서 조회수를 증가시키는 패턴이 있다면 트랜잭션 속성을 재검토해야 한다
- **LazyConnection 오버헤드** — 모든 커넥션 획득에 프록시 레이어가 하나 추가된다. 실제 성능 영향은 미미하지만, 커넥션 관련 예외의 스택트레이스가 길어진다
