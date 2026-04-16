---
name: TOCTOU 중복 체크는 DB 제약에 위임
description: 애플리케이션 레벨 existsBy + save 패턴은 TOCTOU race condition을 유발하므로 DB UNIQUE 제약과 DataIntegrityViolationException 핸들러에만 의존
type: feedback
---

애플리케이션 레벨의 `existsBy...` + `save` 순서는 두 호출 사이에 동시성 갭이 발생하여 TOCTOU race condition을 일으킨다.

**Why:** 두 요청이 동시에 existsBy를 통과한 뒤 둘 다 save를 시도하면 중복 신고가 DB에 삽입됨. 코드 리뷰에서 이 패턴을 Critical로 지적함.

**How to apply:** 애플리케이션 레벨 중복 체크(existsBy + throw DuplicateException)를 제거하고, DB UNIQUE 제약이 발생시키는 `DataIntegrityViolationException`을 `GlobalExceptionHandler`에서 처리하는 방식으로만 의존한다. 제약 이름 상수(`UQ_...`)를 엔티티에 정의하고 핸들러와 테스트에서 공유한다.
