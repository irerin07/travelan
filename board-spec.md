# 게시판 (Board) - PRD

## 1. 개요
여행 커뮤니티의 핵심 콘텐츠 채널. 사용자는 지역(Region) 기반으로 분류된 게시판에 글을 작성하고, 비회원도 자유롭게 열람할 수 있다. 한국형 커뮤니티 UX를 따르며, 첫 단계에서는 운영자 큐레이션 카테고리(지역) 구조로 시작한다. 사용자 생성 서브게시판은 v2 백로그.

## 2. 목표
| 목표 | 설명 |
|------|------|
| 콘텐츠 축적 | 지역별 분류로 검색·탐색이 쉬운 여행 정보 축적 |
| 진입 장벽 최소화 | 비회원도 글 열람 가능 → SEO/유입 강화 |
| 운영 가능한 구조 | 신고·삭제 등 최소 모더레이션 제공 |
| 데이터 영속성 | 탈퇴 사용자의 글도 유지하여 커뮤니티 자산 보존 |

## 3. 사용자 스토리
- 비회원으로서, 특정 지역의 여행 후기를 자유롭게 검색·열람하고 싶다.
- 회원으로서, 내가 다녀온 지역 게시판에 후기와 사진을 올리고 싶다.
- 회원으로서, 내가 작성한 글을 수정·삭제할 수 있길 원한다.
- 회원으로서, 부적절한 글을 신고할 수 있길 원한다.
- 운영자로서, 신고된 글을 검토하고 삭제할 수 있길 원한다.

## 4. 도메인 모델

### 4.1 Region (지역 카테고리)
운영자가 사전 정의하는 1단계 지역 카테고리. v1에서는 사용자 생성 불가.

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| code | String | 식별 코드 (예: `seoul`, `jeju`) — URL slug |
| name | String | 표시 이름 (예: 서울, 제주) |
| description | String | 설명 |
| displayOrder | int | 정렬 순서 |
| active | boolean | 노출 여부 |

**초기 시드 데이터 (총 16개)**:
- 국내 (8개): 서울, 경기, 인천, 강원, 충청, 전라, 경상, 제주
- 인기 해외 (7개): 일본, 중국, 베트남, 태국, 필리핀, 대만, 미국
- 기타 (1개): 기타 해외 — 위 7개국 외 모든 해외 여행지 포괄

`displayOrder`는 국내 → 인기 해외 → 기타 해외 순으로 부여한다.

### 4.2 Post (게시글)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| region | Region (FK) | 소속 지역 게시판 |
| author | User (FK) | 작성자 — 탈퇴해도 유지 (`ON DELETE` 없음) |
| title | String(150) | 제목 |
| content | TEXT | 본문 — Sanitized HTML (DCInside 스타일: 인라인 서식·이미지·링크 허용, 스크립트/이벤트 핸들러 제거) |
| viewCount | long | 조회 수 |
| status | Enum | `PUBLISHED`, `DELETED`, `BLOCKED` |
| createdAt / updatedAt | LocalDateTime | 감사 컬럼 |

### 4.3 PostImage (첨부 이미지)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| post | Post (FK) | 소속 게시글 |
| url | String | 저장 URL |
| originalName | String | 원본 파일명 |
| size | long | 바이트 |
| displayOrder | int | 정렬 순서 |

### 4.4 PostReport (신고)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| post | Post (FK) | 신고 대상 |
| reporter | User (FK) | 신고자 |
| reason | Enum | `SPAM`, `ABUSE`, `ADULT`, `ETC` |
| detail | String(500) | 상세 사유 |
| createdAt | LocalDateTime | 신고 시각 |

## 5. 기능 요구사항

### 5.1 게시판(지역) 목록 조회
| 항목 | 내용 |
|------|------|
| Endpoint | `GET /api/v1/regions` |
| 인증 | 불필요 |
| Response | `active=true`인 Region 목록 (`displayOrder` 정렬) |

### 5.2 게시글 목록 조회
| 항목 | 내용 |
|------|------|
| Endpoint | `GET /api/v1/regions/{regionCode}/posts?page=&size=&sort=` |
| 인증 | 불필요 |
| 정렬 | 최신순(default), 조회순 |
| Response | `Page<PostSummaryResponse>` |

### 5.3 게시글 상세 조회
| 항목 | 내용 |
|------|------|
| Endpoint | `GET /api/v1/posts/{postId}` |
| 인증 | 불필요 |
| 동작 | 조회 시 `viewCount` 증가 (동일 IP/세션 중복 카운트 방지는 v2) |
| Response | `PostDetailResponse` (이미지 포함) |
| 에러 | `DELETED`/`BLOCKED` → `404` |

### 5.4 게시글 작성
| 항목 | 내용 |
|------|------|
| Endpoint | `POST /api/v1/regions/{regionCode}/posts` |
| 인증 | Access Token 필수 |
| Request | `title`, `content`, `imageIds[]` (사전 업로드된 이미지 ID) |
| Response | `201 Created` + `PostDetailResponse` |

### 5.5 게시글 수정
| 항목 | 내용 |
|------|------|
| Endpoint | `PUT /api/v1/posts/{postId}` |
| 인증 | Access Token 필수 (작성자 본인만) |
| Request | `title`, `content`, `imageIds[]` |
| 에러 | 작성자 불일치 → `403` |

### 5.6 게시글 삭제
| 항목 | 내용 |
|------|------|
| Endpoint | `DELETE /api/v1/posts/{postId}` |
| 인증 | Access Token 필수 (작성자 본인 또는 운영자) |
| 동작 | Soft Delete (`status = DELETED`) |
| Response | `204 No Content` |

### 5.7 이미지 업로드
| 항목 | 내용 |
|------|------|
| Endpoint | `POST /api/v1/posts/images` |
| 인증 | Access Token 필수 |
| Request | `multipart/form-data` (단일/다중) |
| 제약 | 파일당 5MB 이하, jpg/png/webp, 게시글당 최대 10장 |
| Response | `[{ id, url }]` (게시글 작성 시 `imageIds`로 첨부) |

### 5.8 게시글 신고
| 항목 | 내용 |
|------|------|
| Endpoint | `POST /api/v1/posts/{postId}/reports` |
| 인증 | Access Token 필수 |
| 제약 | 동일 사용자가 동일 글 중복 신고 불가 |
| Response | `201 Created` |

## 6. 비기능 요구사항
- **탈퇴 사용자 처리**: 탈퇴해도 게시글은 유지. 작성자 표시는 익명화된 닉네임(`탈퇴{N}`)이 그대로 노출됨.
- **이미지 스토리지**: v1은 로컬 디스크 기반. 저장 경로는 `./uploads/posts/{userId}/{yyyy}/{MM}/`. 추후 S3 등으로 교체 가능하도록 `FileStorage` 인터페이스 추상화.
- **HTML Sanitize**: 본문은 저장 시점에 Jsoup `Safelist.relaxed()` 기반 화이트리스트로 sanitize. 스크립트, 이벤트 핸들러(`onclick` 등), `<iframe>` 등 위험 요소 제거. 렌더 시 추가 escape 없이 그대로 표시.
- **SEO**: 비회원 조회 허용 + 의미 있는 URL(`/regions/{code}/posts/{id}`) 지향. SSR/메타태그는 v2.
- **모더레이션**: 운영자 권한(`ROLE_ADMIN`)은 기존 User 엔티티 활용. 신고 검토 화면은 v2.

## 7. 보안 / 권한
| 작업 | 비회원 | 회원 | 작성자 | 운영자 |
|------|--------|------|--------|--------|
| 목록/상세 조회 | ✓ | ✓ | ✓ | ✓ |
| 작성 | ✗ | ✓ | - | ✓ |
| 수정 | ✗ | ✗ | ✓ | ✗ |
| 삭제 | ✗ | ✗ | ✓ | ✓ |
| 신고 | ✗ | ✓ | ✗(자기글) | ✓ |

## 8. v2 백로그 (이번 범위 제외)
- 댓글/대댓글
- 추천/비추천
- 사용자 생성 서브게시판
- 태그 시스템
- 게시판 구독/알림
- 인기 게시글 / 실시간 베스트
- 운영자 신고 검토 대시보드
- 조회수 중복 방지 (IP/세션 기반)
- S3 등 외부 스토리지 연동

## 9. 결정 완료
- [x] 본문 포맷 — **Sanitized HTML** (Jsoup `Safelist.relaxed()` 기반)
- [x] 초기 Region 시드 — **16개** (국내 8 + 인기 해외 7 + 기타 해외 1)
- [x] 이미지 저장 경로 — **`./uploads/posts/{userId}/{yyyy}/{MM}/`**, 정적 서빙은 Spring `WebMvcConfigurer.addResourceHandlers`로 매핑
