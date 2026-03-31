# API 응답 래퍼 비교: ResourcesWrapper vs ApiResponse

---

## 1. 구현 방식 요약

### ResourcesWrapper

```java
// 사용 예
new ResourcesWrapper.Builder(singleObject).build()
new ResourcesWrapper.Builder(page, blockSize).search(searchDto).build()
builder.content("customKey", value).build()
```

```json
// 단일 객체 응답
{
  "content": { "resource": { ... } }
}

// 컬렉션 / 페이징 응답
{
  "content": { "resources": [ ... ] },
  "page": { "currentNumber": 1, "totalPages": 5, ... }
}
```

**핵심 설계 철학:** 콘텐츠의 유형(단일/컬렉션/페이지/슬라이스)을 런타임에 자동 감지하고, 빌더를 통해 `content`, `search`, `page`를 조합해 하나의 봉투(envelope)로 구성한다. 성공/실패 구분은 HTTP 상태 코드에 위임한다.

---

### ApiResponse (현재 프로젝트)

```java
// 사용 예
ApiResponse.ok(singleOrListData)
ApiResponse.ofPage(page, pageNumber)
ApiResponse.error(ErrorResponse.of("NOT_FOUND", "리소스 없음"))
```

```json
// 단일 / 목록 응답
{ "success": true, "data": { ... } }

// 페이징 응답
{
  "success": true,
  "data": [ ... ],
  "page": { "page": 1, "size": 20, "totalElements": 100, "totalPages": 5 }
}

// 에러 응답
{
  "success": false,
  "error": { "code": "VALIDATION_ERROR", "message": "...", "errors": [ ... ] }
}
```

**핵심 설계 철학:** 모든 응답에 `success` 필드로 명시적 성공/실패를 표현하고, 정적 팩토리 메서드로 응답 유형을 컴파일 타임에 고정한다. HTTP 상태 코드와 응답 바디 모두에서 결과를 명확히 전달한다.

---

## 2. 구현 방식이 다른 이유

### ResourcesWrapper가 이 방식을 선택한 이유
- **서버 사이드 렌더링(MVC) 환경** 또는 내부 API처럼 클라이언트와 서버가 같은 코드베이스를 공유하는 환경에서 만들어진 패턴이다. HTTP 상태 코드만으로 성공/실패를 충분히 구분할 수 있다고 가정한다.
- 여러 종류의 페이지 래퍼(`PageWrapper`, `SliceWrapper`)와 검색 조건(`search`)을 **하나의 응답 구조에 유연하게 조합**하기 위해 빌더 패턴을 채택했다.
- 콘텐츠 키(`resource` vs `resources`)를 런타임에 자동 결정함으로써 **호출 코드를 단순하게 유지**한다.

### ApiResponse가 이 방식을 선택한 이유
- **REST API / JSON API** 환경에서는 클라이언트(모바일 앱, 프론트엔드 SPA)가 HTTP 상태 코드와 별개로 **바디에서도 성공/실패를 판단**하는 것이 일반적이다. `success` 필드가 그 역할을 한다.
- 정적 팩토리 메서드(`ok`, `ofPage`, `error`)로 응답 유형을 **컴파일 타임에 고정**하여 잘못된 조합(예: `success: false`에 `data` 포함)을 방지한다.
- 프로젝트 컨벤션(`new` 연산자 금지, 불변 객체 선호)을 따른다.

---

## 3. 문제점

### ResourcesWrapper의 문제점

#### 3-1. 타입 안전성 부재
`content`가 `Map<String, Object>`이므로 컴파일러가 내용을 검증할 수 없다. 잘못된 타입을 넣어도 런타임에야 발견된다.
```java
// 컴파일 오류 없음 — 런타임에 ClassCastException 가능
builder.content("resource", 12345).build();
```

#### 3-2. 성공/실패 구분 불명확
HTTP 상태 코드만으로 결과를 판단한다. 하지만 네트워크 미들웨어(프록시, CDN, API 게이트웨이)가 상태 코드를 변경하거나, 클라이언트가 상태 코드를 먼저 확인하지 않는 경우 바디만으로는 성공 여부를 알 수 없다.

#### 3-3. 표준화된 에러 구조 없음
에러 응답 형식이 `ResourcesWrapper` 자체에 정의되어 있지 않다. 에러 처리가 분리되어 있을 경우 성공 응답과 에러 응답의 구조가 완전히 달라진다.

#### 3-4. `new` 연산자 강제
```java
new ResourcesWrapper.Builder(data).build() // 반드시 new 사용
```
빌더 진입점이 생성자이기 때문에 `new` 없이 사용할 수 없다.

#### 3-5. 가변 객체
필드에 `final`이 없고 `@NoArgsConstructor`가 `public`이다. 빈 인스턴스 생성 후 직렬화하면 빈 응답이 나갈 수 있다.

#### 3-6. 중첩 구조의 복잡성
```json
{ "content": { "resources": [ ... ] } }
```
콘텐츠 배열이 `content.resources`라는 2단계 중첩 아래에 있어 클라이언트 파싱 코드가 복잡해진다.

#### 3-7. 외부 의존성
`apache commons lang3`(`ToStringBuilder`)에 의존한다. 이 라이브러리가 없는 환경에서는 사용할 수 없다.

---

### ApiResponse의 문제점

#### ~~3-8. 빌더 없음 — 복합 응답 구성 불가~~ (해결됨)
`search` 필드와 `Builder<T>` 내부 클래스를 추가하여 `data + page + search`를 자유롭게 조합할 수 있다.
```java
ApiResponse.<List<String>>builder()
    .data(page.getContent())
    .page(PageMeta.from(page, 1))
    .search(searchDto)
    .build();
```

#### 3-9. `Slice<T>` 미지원
`ofPage(Page<T>, int)`만 있고 `Slice<T>`(무한 스크롤용) 팩토리가 없다.

#### 3-10. 보안 관련 JSON 하드코딩
`SecurityConfig`의 401/403 응답이 문자열 상수로 하드코딩되어 있다. `ApiResponse`/`ErrorResponse` 구조가 바뀌면 이 상수도 직접 수정해야 해서 불일치가 발생할 수 있다.
```java
private static final String UNAUTHORIZED_BODY =
    "{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",...}}";
```

#### 3-11. 단일/컬렉션 자동 구분 없음
`ok(list)`, `ok(singleObject)` 모두 `ok(T)`를 쓰므로 응답 형태가 `T`의 실제 타입에 따라 달라진다. 클라이언트는 항상 API 문서를 확인해야 한다.

#### 3-12. `ofPage` 반환 타입 고정
`ofPage`는 항상 `ApiResponse<List<T>>`를 반환한다. 향후 `data` 안에 리스트 외의 추가 정보가 필요해지면 구조 변경이 필요하다.

---

## 4. 장단점 비교표

| 항목 | ResourcesWrapper | ApiResponse |
|---|---|---|
| **타입 안전성** | ❌ `Map<String, Object>` | ✅ 제네릭 `T` |
| **성공/실패 명시** | ❌ HTTP 상태 코드 의존 | ✅ `success` 필드 |
| **에러 구조 표준화** | ❌ 별도 정의 없음 | ✅ `ErrorResponse` 통합 |
| **불변성** | ❌ `final` 없음 | ✅ 모든 필드 `final` |
| **복합 응답 구성** | ✅ 빌더로 자유롭게 조합 | ✅ Builder + 정적 팩토리 병행 |
| **검색 조건 포함** | ✅ `.search(dto)` | ✅ `.search(dto)` |
| **페이징** | ✅ `Page`, `Slice` 모두 지원 | ⚠️ `Page`만 지원 |
| **new 연산자** | ❌ 빌더 진입점이 생성자 | ✅ 정적 팩토리만 사용 |
| **JSON 중첩 깊이** | ❌ `content.resources[i]` | ✅ `data[i]` |
| **외부 의존성** | ❌ commons-lang3 필요 | ✅ 없음 |
| **컴파일 타임 검증** | ❌ 런타임 감지 | ✅ 잘못된 조합 불가 |
| **클라이언트 파싱 편의성** | ❌ 응답 키 가변(`resource`/`resources`) | ✅ 항상 `data` |
| **보안 응답 일관성** | N/A | ⚠️ 하드코딩 상수 |

---

## 5. 각 방식이 적합한 상황

### ResourcesWrapper가 적합한 경우
- **서버 사이드 렌더링(Thymeleaf, JSP)** 환경에서 컨트롤러가 뷰 모델을 구성할 때
- **내부 관리 도구**처럼 에러 처리를 HTTP 상태 코드에 위임해도 충분한 환경
- **동적인 응답 구조**가 필요할 때 — 검색 조건, 커스텀 메타데이터를 상황에 따라 포함/제외

### ApiResponse가 적합한 경우
- **모바일 앱 / SPA 프론트엔드**와 통신하는 REST API — 클라이언트가 `success` 필드로 빠르게 분기
- **공개 API** — 예측 가능하고 일관된 응답 구조가 중요할 때
- **표준화된 에러 처리**가 필요한 경우 — `code`, `message`, `errors` 포맷을 모든 에러에 통일

---

## 6. 현재 프로젝트에서 ApiResponse를 선택한 근거

Travelan은 모바일/SPA 클라이언트를 대상으로 하는 REST API이므로:

1. 클라이언트가 `success` 필드 하나로 성공/실패를 판단할 수 있어 네트워크 오류와 비즈니스 오류를 동일한 코드 경로로 처리 가능
2. `ErrorResponse`의 `code` 필드로 클라이언트가 에러 종류별로 분기 처리 가능 (예: `DUPLICATE` → "이미 사용 중" 토스트, `VALIDATION_ERROR` → 필드별 에러 표시)
3. 응답 구조가 고정되어 있어 Swagger/OpenAPI 문서화가 명확함
4. 중첩 없는 `data` 필드로 클라이언트 파싱 코드가 단순함

다만, 추후 검색 API에서 검색 조건을 응답에 포함해야 하거나 무한 스크롤(`Slice`)을 도입할 경우 `ofSearch()`, `ofSlice()` 팩토리를 추가하는 방향으로 확장하면 된다.
