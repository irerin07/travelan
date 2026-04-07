# 회원 탈퇴 - PRD

## 1. 개요
로그인한 회원이 본인 계정을 탈퇴할 수 있는 기능. 개인정보보호 관점에서 즉시 익명화(Soft Delete)하며, 동일 이메일/휴대폰/닉네임의 재사용을 가능하게 한다.

## 2. 목표
| 목표 | 설명 |
|------|------|
| 사용자 권리 보장 | 회원이 언제든지 본인 의사로 탈퇴 가능 |
| 개인정보 즉시 익명화 | 이메일/휴대폰/닉네임을 즉시 익명 값으로 치환 |
| 재가입 가능 | 탈퇴한 이메일/휴대폰/닉네임은 다른 사용자가 재사용 가능 |
| 이력 추적 | 탈퇴 시점·원본 이메일을 감사 로그로 보존 |

## 3. 사용자 스토리
- 회원으로서, 더 이상 서비스를 이용하지 않을 경우 내 계정을 삭제하고 싶다.
- 회원으로서, 탈퇴 시 내 개인정보가 즉시 익명화되길 원한다.
- 회원으로서, 탈퇴 후 동일 이메일로 새 계정을 다시 만들 수 있길 원한다.

## 4. 기능 요구사항

### 4.1 회원 탈퇴
| 항목 | 내용 |
|------|------|
| Endpoint | `DELETE /api/v1/users/me` |
| 인증 | Access Token 필수 |
| Request | (없음) |
| Response | `204 No Content` |

### 처리 흐름
1. Access Token에서 `userId` 추출
2. 회원 조회 — 미존재 시 `401`
3. 이미 `WITHDRAWN` 상태면 `404 Not Found` (탈퇴된 회원은 존재하지 않는 리소스로 간주)
4. `User.withdraw(clock)` 호출 → 익명화 + 상태 변경
   - `originalEmail` 보존
   - `email = "withdrawn_{id}@deleted"`
   - `phone = "del_{id}"`
   - `nickname = "탈퇴{id % 99_999_999}"`
   - `status = WITHDRAWN`
   - `withdrawnAt = now()`
5. 해당 회원의 모든 RefreshToken revoke (`revokeAllByUserId`)
6. UserHistory에 `WITHDRAWAL` 이벤트 1건 기록
7. Refresh Token Cookie 즉시 만료 (`Max-Age=0`)
8. `204 No Content` 반환

### 4.2 탈퇴 후 동작
- **재로그인 차단**: `AuthService.login()`은 `status != ACTIVE` 시 `401` 반환 (이미 구현됨)
- **재가입 허용**: 익명화된 이메일/휴대폰/닉네임은 unique 제약을 위반하지 않음
- **개인정보**: `originalEmail` 외 식별 정보는 익명 문자열로 치환되어 본인 식별 불가

## 5. 보안/제약
- 본인만 탈퇴 가능 (Access Token의 `userId` 기반)
- 관리자가 강제 탈퇴시키는 기능은 본 스펙 범위 외
- 탈퇴 후 즉시 모든 세션 무효화

## 6. 데이터 모델 (기존 컬럼 재사용)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| status | ENUM | `WITHDRAWN`으로 변경 |
| original_email | VARCHAR(255) NULL | 탈퇴 전 원본 이메일 보존 |
| withdrawn_at | DATETIME NULL | 탈퇴 시각 |

> V8 마이그레이션으로 이미 존재. 신규 마이그레이션 불필요.

## 7. API 명세

### DELETE /api/v1/users/me
**Request Header**
```
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`
- Body 없음
- `Set-Cookie: refreshToken=; Max-Age=0; Path=/api/v1/auth; ...`

**오류 응답**
- `401 Unauthorized` — 토큰 없음/무효
- `404 Not Found` — 이미 탈퇴된 회원 또는 존재하지 않는 회원
