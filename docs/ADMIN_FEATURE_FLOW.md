# 관리자 대시보드 기능 흐름 순서도

본 문서는 관리자 대시보드에서 제공하는 5개 도메인(회원 / 이벤트 / 공지 / 문의 / 시설예약)의
**플러터 → 스프링 부트** 전 구간 동작 흐름을 파일 단위로 정리한 문서입니다.

- 프론트엔드: **Flutter** (`Flutter-Front/`)
- 백엔드: **Spring Boot 3 + JPA** (`Spring-Back/SpringBasic/api5012/`)
- 통신: REST + JSON / `Authorization: Bearer {accessToken}`
- 공통 Base URL: `ApiConstants.springBaseUrl = http://10.0.2.2:8080/api`

---

## 공통 아키텍처 요약

### 프론트엔드 공통 흐름
```
┌─────────────────────┐    ┌────────────────┐    ┌─────────────────┐
│ 관리자 대시보드 카드 │ → │ Admin 화면 진입 │ → │ initState() →   │
│ (admin_screen)      │    │ (Navigator)    │    │ _fetchXxx() 호출│
└─────────────────────┘    └────────────────┘    └─────────────────┘
                                                            ↓
                                            flutter_secure_storage
                                            에서 accessToken 로드
                                                            ↓
                                            http.get/post/put/delete
                                            + Authorization 헤더
                                                            ↓
                                            ┌────────────────────────┐
                                            │  Spring REST API 호출  │
                                            └────────────────────────┘
                                                            ↓
                                    JSON 파싱 → setState() → ListView 재빌드
                                                            ↓
                                            AlertDialog(StatefulBuilder)
                                            로 등록/수정/삭제 다이얼로그 표시
                                                            ↓
                                    ScaffoldMessenger.showSnackBar 로 결과 통지
```

### 백엔드 공통 흐름
```
HTTP Request (JWT 포함)
        ↓
[JwtCheckFilter] — Authorization 헤더 검증, 인증 객체 생성
        ↓
[@RestController]  — DTO 바인딩, @PathVariable/@RequestBody 매핑
        ↓
[@Service (interface) → @ServiceImpl] — 비즈니스 로직, 트랜잭션 경계
        ↓
[JpaRepository] — findById / save / delete / findAll(Pageable)
        ↓
[Entity (도메인 메서드)] — changeXxx(), updateInfo(), approve() 등
        ↓
@Transactional 더티체킹 → UPDATE / INSERT / DELETE 자동 반영
        ↓
ResponseEntity<Map<String, Object>> 반환 (result/message/id 등)
```

---

## 1. 회원 관리 (Member)

### 제공 기능
- 회원 목록 조회 / 검색
- 회원 정보 수정 (이름, 이메일, 지역, 권한)
- 회원 삭제

### 프론트엔드 흐름

```
admin_screen.dart (관리자 대시보드 카드)
            │  "회원 관리" 탭
            ▼
Navigator.pushNamed('/adminMember')
            │
            ▼
my_app.dart (routes 매핑)
            │
            ▼
admin_member_screen.dart
 ├─ initState() ──► _fetchMembers()
 │                   └─ GET /api/member/list
 ├─ TextField onChanged ──► _searchQuery 로컬 필터링
 ├─ [수정] IconButton ──► _showEditDialog(m)
 │                         └─ AlertDialog(StatefulBuilder)
 │                         └─ 저장 ──► _updateMember(id, body)
 │                                      └─ PUT /api/member/admin/{id}
 └─ [삭제] IconButton ──► _confirmDelete(m)
                           └─ AlertDialog(확인)
                           └─ 확인 ──► _deleteMember(id)
                                        └─ DELETE /api/member/{id}
```

**관련 플러터 파일**
| 파일 | 역할 |
|------|------|
| `Flutter-Front/lib/my_app.dart` | 라우트 `/adminMember` 등록 |
| `Flutter-Front/lib/screen/admin/admin_screen.dart` | 대시보드 진입 |
| `Flutter-Front/lib/screen/admin/admin_member_screen.dart` | 목록/수정/삭제 다이얼로그 + HTTP 호출 |
| `Flutter-Front/lib/const/api_constants.dart` | `springBaseUrl` 상수 |

### 백엔드 흐름

```
HTTP Request
     │
     ▼
MemberController.java
 ├─ GET    /api/member/list            ──► getMemberList()
 ├─ PUT    /api/member/admin/{id}      ──► adminUpdateMember(id, dto)
 └─ DELETE /api/member/{id}            ──► deleteMember(id)
     │
     ▼
MemberLibraryService.java (interface)
     │
     ▼
MemberLibraryServiceImpl.java
 ├─ getMemberList()     → repository.findAll() → Stream.map(DTO)
 ├─ adminUpdateMember() → findById → member.updateInfoByAdmin(...) → 더티체킹
 └─ deleteMember()      → findById → repository.delete()
     │
     ▼
MemberRepository.java (extends JpaRepository<Member, Long>)
     │
     ▼
Member.java (Entity, domain methods: updateInfoByAdmin, changeRole 등)
```

**관련 백엔드 파일**
| 파일 | 역할 |
|------|------|
| `controller/MemberController.java` | REST 엔드포인트 |
| `service/library/MemberLibraryService.java` | 서비스 인터페이스 |
| `service/library/MemberLibraryServiceImpl.java` | 비즈니스 로직 구현 |
| `repository/library/MemberRepository.java` | JPA 리포지토리 |
| `domain/library/Member.java` | JPA 엔티티 + 도메인 메서드 |
| `dto/library/MemberDTO.java` | DTO |
| `config/CustomSecurityConfig.java` / `security/filter/JwtCheckFilter.java` | JWT 인증 필터 |

---

## 2. 이벤트 관리 (Event)

### 제공 기능
- 이벤트 목록 조회
- 이벤트 등록 (category, title, content, eventDate, place, maxParticipants)
- 이벤트 수정 (+ 상태 OPEN/CLOSED)
- 이벤트 삭제

### 프론트엔드 흐름

```
admin_screen.dart → Navigator.pushNamed('/adminEvent')
            ▼
admin_event_screen.dart
 ├─ initState() ──► _fetchEvents()
 │                   └─ GET /api/event?page=0&size=100
 ├─ FAB "이벤트 등록" ──► _showFormDialog()
 │                        └─ AlertDialog(StatefulBuilder)
 │                           ├─ TextField(title/category/content/place/maxParticipants)
 │                           ├─ DatePicker(eventDate)
 │                           └─ 저장 ──► _createEvent(body)
 │                                       └─ POST /api/event
 ├─ [수정] IconButton ──► _showFormDialog(existing: e)
 │                         └─ Dropdown(status: OPEN/CLOSED) 추가 표시
 │                         └─ 저장 ──► _updateEvent(id, body)
 │                                      └─ PUT /api/event/{id}
 └─ [삭제] IconButton ──► _confirmDelete(e)
                           └─ 확인 ──► _deleteEvent(id)
                                        └─ DELETE /api/event/{id}
```

**관련 플러터 파일**
| 파일 | 역할 |
|------|------|
| `Flutter-Front/lib/my_app.dart` | 라우트 `/adminEvent` |
| `Flutter-Front/lib/screen/admin/admin_event_screen.dart` | CRUD 다이얼로그 + HTTP |

### 백엔드 흐름

```
EventController.java
 ├─ GET    /api/event?page&size       ──► getEventList(pageable)
 ├─ POST   /api/event                 ──► createEvent(dto)
 ├─ PUT    /api/event/{id}            ──► updateEvent(id, dto)
 └─ DELETE /api/event/{id}            ──► deleteEvent(id)
     │
     ▼
EventService.java (interface)
     │
     ▼
EventServiceImpl.java
 ├─ getEventList() → libraryEventRepository.findAll(pageable).map(LibraryEventDTO::fromEntity)
 ├─ createEvent()  → LibraryEvent.builder()... → repository.save()
 ├─ updateEvent()  → findById → event.updateInfo(...) → 상태 있으면 changeStatus() → 더티체킹
 └─ deleteEvent()  → findById → repository.delete()
     │
     ▼
LibraryEventRepository.java
     │
     ▼
LibraryEvent.java (Entity) + LibraryEventDTO.java
```

**관련 백엔드 파일**
| 파일 | 역할 |
|------|------|
| `controller/library/EventController.java` | REST 엔드포인트 |
| `service/library/EventService.java` | 인터페이스 |
| `service/library/EventServiceImpl.java` | 구현체 |
| `repository/library/LibraryEventRepository.java` | JPA 리포지토리 |
| `domain/library/LibraryEvent.java` | 엔티티 |
| `dto/library/LibraryEventDTO.java` | DTO |

---

## 3. 공지사항 관리 (Notice)

### 제공 기능
- 공지 목록 조회 (상단 고정 우선)
- 공지 등록 (title, content, writer, topFixed)
- 공지 수정
- 공지 삭제 (첨부 이미지 cascade 삭제)

### 프론트엔드 흐름

```
admin_screen.dart → Navigator.pushNamed('/adminNotice')
            ▼
admin_notice_screen.dart
 ├─ initState() ──► _fetchNotices()
 │                   └─ GET /api/notice?page=0&size=100
 ├─ FAB "공지 등록" ──► _showFormDialog()
 │                       └─ AlertDialog(StatefulBuilder)
 │                          ├─ TextField(title/writer/content)
 │                          ├─ SwitchListTile("상단 고정")
 │                          └─ 저장 ──► _createNotice(body)
 │                                      └─ POST /api/notice
 ├─ [수정] IconButton ──► _showFormDialog(existing: n)
 │                         └─ 저장 ──► _updateNotice(id, body)
 │                                      └─ PUT /api/notice/{id}
 └─ [삭제] IconButton ──► _confirmDelete(n)
                           └─ 확인 ──► _deleteNotice(id)
                                        └─ DELETE /api/notice/{id}
```

**관련 플러터 파일**
| 파일 | 역할 |
|------|------|
| `Flutter-Front/lib/my_app.dart` | 라우트 `/adminNotice` |
| `Flutter-Front/lib/screen/admin/admin_notice_screen.dart` | CRUD 화면 |

### 백엔드 흐름

```
NoticeController.java
 ├─ GET    /api/notice?page&size      ──► getNoticeList(pageable)
 ├─ GET    /api/notice/{id}           ──► getNotice(id)  (상세: 이미지 포함)
 ├─ POST   /api/notice                ──► createNotice(dto)
 ├─ PUT    /api/notice/{id}           ──► updateNotice(id, dto)
 └─ DELETE /api/notice/{id}           ──► deleteNotice(id)
     │
     ▼
NoticeService.java → NoticeServiceImpl.java
 ├─ getNotices()   → topFixed 우선 + 일반 페이지네이션 → PageImpl 합성
 ├─ getNoticeById()→ findWithImagesById()  (JOIN FETCH, N+1 방지)
 ├─ createNotice() → Notice.builder() + addImage() → save()
 ├─ updateNotice() → findWithImagesById → changeTitle/Content/TopFixed + clearImages + re-add
 └─ deleteNotice() → findById → delete()  (cascade=ALL + orphanRemoval)
     │
     ▼
NoticeRepository.java  (findByTopFixedTrueOrderByRegDateDesc, findWithImagesById)
     │
     ▼
Notice.java (Entity, 도메인 메서드) + NoticeImage.java (cascade child)
```

**관련 백엔드 파일**
| 파일 | 역할 |
|------|------|
| `controller/library/NoticeController.java` | REST 엔드포인트 |
| `service/library/NoticeService.java` | 인터페이스 |
| `service/library/NoticeServiceImpl.java` | 상단고정/페이지네이션 로직 |
| `repository/library/NoticeRepository.java` | JPA 리포지토리 |
| `domain/library/Notice.java` | 엔티티 |
| `domain/library/NoticeImage.java` | 첨부 이미지 (cascade) |
| `dto/library/NoticeDTO.java` | DTO |

---

## 4. 문의 관리 (Inquiry)

### 제공 기능
- 문의 목록 조회 (관리자는 전체, 회원은 본인 + 비밀글 제외)
- 상세 조회 (답변 리스트 포함)
- 문의 수정 (title, content, secret)
- 문의 삭제 (답변 cascade 삭제)
- 답변 작성 (replyText, replier)

### 프론트엔드 흐름

```
admin_screen.dart → Navigator.pushNamed('/adminInquiry')
            ▼
admin_inquiry_screen.dart
 ├─ initState() ──► _fetchInquiries()
 │                   └─ GET /api/inquiry?page=0&size=200
 │                       (viewerMemberId 파라미터 없음 → 관리자 전체 조회)
 ├─ [답변] IconButton ──► _showReplyDialog(inq)
 │                         ├─ _fetchDetail(id) ──► GET /api/inquiry/{id}
 │                         │                       (기존 답변 로드)
 │                         └─ 저장 ──► _addReply(id, text, replier)
 │                                      └─ POST /api/inquiry/{id}/reply
 ├─ [수정] IconButton ──► _showEditDialog(inq)
 │                         ├─ _fetchDetail(id) ──► GET /api/inquiry/{id}
 │                         └─ 저장 ──► _updateInquiry(id, body)
 │                                      └─ PUT /api/inquiry/{id}
 └─ [삭제] IconButton ──► _confirmDelete(inq)
                           └─ 확인 ──► _deleteInquiry(id)
                                        └─ DELETE /api/inquiry/{id}
```

**관련 플러터 파일**
| 파일 | 역할 |
|------|------|
| `Flutter-Front/lib/my_app.dart` | 라우트 `/adminInquiry` |
| `Flutter-Front/lib/screen/admin/admin_inquiry_screen.dart` | 수정/삭제/답변 다이얼로그 |

### 백엔드 흐름

```
InquiryController.java
 ├─ GET    /api/inquiry?page&size            ──► getInquiryList(pageable, viewerId)
 ├─ GET    /api/inquiry/{id}                 ──► getInquiry(id)  (replies 포함)
 ├─ PUT    /api/inquiry/{id}                 ──► updateInquiry(id, dto)
 ├─ DELETE /api/inquiry/{id}                 ──► deleteInquiry(id)
 └─ POST   /api/inquiry/{id}/reply           ──► addReply(id, replyDto)
     │
     ▼
InquiryService.java → InquiryServiceImpl.java
 ├─ getInquiries()    → viewerId 에 따라 관리자/회원 쿼리 분기
 ├─ getInquiryById()  → findWithRepliesById() (JOIN FETCH)
 ├─ updateInquiry()   → findById → inquiry.updateInfo(title, content, secret)
 ├─ deleteInquiry()   → findById → delete() (cascade=ALL + orphanRemoval → Reply 삭제)
 └─ addReply()        → findById → inquiry.addReply(reply) → save
     │
     ▼
InquiryRepository.java / ReplyRepository.java
     │
     ▼
Inquiry.java (부모, List<Reply> cascade)  +  Reply.java (자식)
```

**관련 백엔드 파일**
| 파일 | 역할 |
|------|------|
| `controller/library/InquiryController.java` | REST 엔드포인트 |
| `service/library/InquiryService.java` | 인터페이스 |
| `service/library/InquiryServiceImpl.java` | 권한 분기 + 답변 로직 |
| `repository/library/InquiryRepository.java` | JPA 리포지토리 |
| `repository/library/ReplyRepository.java` | 답변 리포지토리 |
| `domain/library/Inquiry.java` | 엔티티 (답변 cascade) |
| `domain/library/Reply.java` | 답변 엔티티 |
| `dto/library/InquiryDTO.java` / `ReplyDTO.java` | DTO |

---

## 5. 시설예약 관리 (Apply)

### 제공 기능
- 전체 예약 조회 (회원명 포함)
- 관리자 직접 예약 등록 (memberId 지정, 상태 선택 가능)
- 예약 정보 수정 (상태 포함)
- 예약 삭제
- 대기중 예약 승인 / 반려

### 프론트엔드 흐름

```
admin_screen.dart → Navigator.pushNamed('/adminFacility')
            ▼
admin_facility_screen.dart
 ├─ initState() ──► _fetchApplies()
 │                   └─ GET /api/apply?page=0&size=200
 ├─ FAB "예약 등록" ──► _showFormDialog()
 │                       └─ AlertDialog(StatefulBuilder)
 │                          ├─ TextField(memberId)
 │                          ├─ TextField(applicantName)
 │                          ├─ Dropdown(facilityType: 세미나실/스터디룸/강당)
 │                          ├─ TextField(phone/participants)
 │                          ├─ DatePicker(reserveDate)
 │                          ├─ Dropdown(status: PENDING/APPROVED/REJECTED)
 │                          └─ 저장 ──► _createApplyAsAdmin(memberId, body)
 │                                      └─ POST /api/apply/admin?memberId={id}
 ├─ PENDING 항목:
 │   ├─ [승인] ──► _handleStatusAction(a, 'approve')
 │   │             └─ PUT /api/apply/{id}/approve
 │   └─ [반려] ──► _handleStatusAction(a, 'reject')
 │                 └─ PUT /api/apply/{id}/reject
 ├─ [수정] IconButton ──► _showFormDialog(existing: a)
 │                         └─ 저장 ──► _updateApply(id, body)
 │                                      └─ PUT /api/apply/{id}
 └─ [삭제] IconButton ──► _confirmDelete(a)
                           └─ 확인 ──► _deleteApply(id)
                                        └─ DELETE /api/apply/{id}
```

**관련 플러터 파일**
| 파일 | 역할 |
|------|------|
| `Flutter-Front/lib/my_app.dart` | 라우트 `/adminFacility` |
| `Flutter-Front/lib/screen/admin/admin_facility_screen.dart` | CRUD + 승인/반려 화면 |

### 백엔드 흐름

```
ApplyController.java
 ├─ GET    /api/apply?page&size                ──► getAllApplies(pageable)
 ├─ POST   /api/apply/admin?memberId={id}      ──► createApplyAsAdmin(dto, memberId)
 ├─ PUT    /api/apply/{id}                     ──► updateApply(id, dto)
 ├─ DELETE /api/apply/{id}                     ──► deleteApply(id)
 ├─ PUT    /api/apply/{id}/approve             ──► approveApply(id)
 └─ PUT    /api/apply/{id}/reject              ──► rejectApply(id)
     │
     ▼
ApplyService.java → ApplyServiceImpl.java
 ├─ getAllApplies()       → applyRepository.findAll(pageable).map(ApplyDTO::fromEntity)
 ├─ createApplyAsAdmin()  → memberRepository.findById → 중복검사(existsByMemberIdAndReserveDateAndStatus)
 │                          → Apply.builder() → status 지정 있으면 changeStatus() → save
 ├─ updateApply()         → findById → apply.updateInfo(...) → status 있으면 changeStatus() → 더티체킹
 ├─ deleteApply()         → findById → delete()
 ├─ approveApply()        → findById → isPending() 체크 → apply.approve()
 └─ rejectApply()         → findById → isPending() 체크 → apply.reject()
     │
     ▼
ApplyRepository.java  +  MemberRepository.java
 └─ existsByMemberIdAndReserveDateAndStatus(memberId, date, status)
     │
     ▼
Apply.java (Entity)
 ├─ 필드: member, applicantName, facilityType, phone, participants,
 │         reserveDate, status(PENDING/APPROVED/REJECTED), regDate
 └─ 도메인 메서드: approve(), reject(), isPending(),
                   updateInfo(...), changeStatus(...)
```

**관련 백엔드 파일**
| 파일 | 역할 |
|------|------|
| `controller/library/ApplyController.java` | REST 엔드포인트 (신청/승인/반려/관리자 CRUD) |
| `service/library/ApplyService.java` | 인터페이스 |
| `service/library/ApplyServiceImpl.java` | 중복검사 + 상태전이 로직 |
| `repository/library/ApplyRepository.java` | JPA 리포지토리 |
| `repository/library/MemberRepository.java` | 회원 조회 |
| `domain/library/Apply.java` | 엔티티 + 도메인 메서드 |
| `dto/library/ApplyDTO.java` | DTO (JSON `yyyy-MM-dd` 포맷) |

---

## 전체 엔드포인트 요약표

| 도메인 | Method | URL | 컨트롤러 | 서비스 메서드 |
|--------|--------|-----|----------|---------------|
| 회원 | GET | `/api/member/list` | MemberController | getMemberList |
| 회원 | PUT | `/api/member/admin/{id}` | MemberController | adminUpdateMember |
| 회원 | DELETE | `/api/member/{id}` | MemberController | deleteMember |
| 이벤트 | GET | `/api/event` | EventController | getEventList |
| 이벤트 | POST | `/api/event` | EventController | createEvent |
| 이벤트 | PUT | `/api/event/{id}` | EventController | updateEvent |
| 이벤트 | DELETE | `/api/event/{id}` | EventController | deleteEvent |
| 공지 | GET | `/api/notice` | NoticeController | getNotices |
| 공지 | GET | `/api/notice/{id}` | NoticeController | getNoticeById |
| 공지 | POST | `/api/notice` | NoticeController | createNotice |
| 공지 | PUT | `/api/notice/{id}` | NoticeController | updateNotice |
| 공지 | DELETE | `/api/notice/{id}` | NoticeController | deleteNotice |
| 문의 | GET | `/api/inquiry` | InquiryController | getInquiries |
| 문의 | GET | `/api/inquiry/{id}` | InquiryController | getInquiryById |
| 문의 | PUT | `/api/inquiry/{id}` | InquiryController | updateInquiry |
| 문의 | DELETE | `/api/inquiry/{id}` | InquiryController | deleteInquiry |
| 문의 | POST | `/api/inquiry/{id}/reply` | InquiryController | addReply |
| 시설예약 | GET | `/api/apply` | ApplyController | getAllApplies |
| 시설예약 | POST | `/api/apply/admin?memberId=` | ApplyController | createApplyAsAdmin |
| 시설예약 | PUT | `/api/apply/{id}` | ApplyController | updateApply |
| 시설예약 | DELETE | `/api/apply/{id}` | ApplyController | deleteApply |
| 시설예약 | PUT | `/api/apply/{id}/approve` | ApplyController | approveApply |
| 시설예약 | PUT | `/api/apply/{id}/reject` | ApplyController | rejectApply |

---

## 공통 인증 플로우

```
Flutter
 └─ flutter_secure_storage.read(key: 'accessToken')
     └─ http 요청 헤더에 Authorization: Bearer {token} 추가
          │
          ▼
Spring Boot
 └─ SecurityFilterChain
     └─ JwtCheckFilter.doFilterInternal()
         ├─ Authorization 헤더에서 토큰 파싱
         ├─ JWTUtil.validateToken() → Claims 추출
         ├─ UsernamePasswordAuthenticationToken 생성
         └─ SecurityContextHolder 에 저장
              │
              ▼
         컨트롤러 진입 (role 체크)
```

**관련 파일**
| 파일 | 역할 |
|------|------|
| `Flutter-Front/lib/screen/login_screen.dart` | 로그인 시 accessToken 저장 |
| `config/CustomSecurityConfig.java` | SecurityFilterChain, CORS, 인증 필터 등록 |
| `security/filter/JwtCheckFilter.java` | JWT 검증 필터 |
| `util/JWTUtil.java` | JWT 생성/검증 유틸 |

---

## 통합 테스트

| 테스트 | 경로 | 검증 대상 |
|--------|------|-----------|
| `ApplyServiceIntegrationTest` | `src/test/java/com/busanit501/api5012/service/library/ApplyServiceIntegrationTest.java` | 시설예약 관리자 CRUD + 승인/반려 (12건) |
| `BookServiceIntegrationTest` | 동일 폴더 | 도서 상태 변경 → Rental 동기화 |
| `RentalServiceIntegrationTest` | 동일 폴더 | 대여/반납 흐름 |

실행:
```bash
cd Spring-Back/SpringBasic/api5012
./gradlew test --tests "com.busanit501.api5012.service.library.ApplyServiceIntegrationTest"
```
