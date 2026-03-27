# 부산도서관 관리 시스템 - 작업 체크리스트

> **프로젝트**: 최종 프로젝트 샘플 — 부산도서관 관리 시스템
> **목적**: 학생 지도용 샘플 프로젝트 (Flutter + Spring Boot + Flask AI)
> **최종 수정**: 2026-03-27
> **작업 상태**: 진행 중

---

## 🏗️ 개발 패턴 및 아키텍처 (Architecture & Patterns)

### 1. Spring Boot 백엔드 (REST API)
- **계층형 아키텍처 (Layered Architecture)** 적용
  - `Controller`: REST API 엔드포인트 담당, 클라이언트 요청 및 JSON 응답 처리
  - `Service` & `ServiceImpl`: 비즈니스 로직 처리 (인터페이스와 구현체를 분리하여 결합도 낮춤)
  - `Repository`: Spring Data JPA를 통한 데이터베이스 접근 및 쿼리 처리
  - `Entity` & `DTO`: 데이터베이스 매핑 객체(Entity)와 외부 데이터 전송 객체(DTO)를 분리하여 의존성을 줄이고 보안성을 강화

### 2. Flutter 프론트엔드 (UI & 상태 관리)
- **Provider 패턴 기반 상태 관리 (MVVM 유사 구조)** 적용
  - `Model`: 데이터 객체 구조 정의 및 JSON 변환 지원 (e.g., `member_model.dart`)
  - `Controller (Provider)`: 화면 비즈니스 로직 처리, 상태 관리, API 호출 담당 (`ChangeNotifier` 사용)
  - `Screen`: 사용자 UI 구현, Provider 상태를 구독 및 렌더링
- **설정 정보 분리**: `const/` 디렉터리에 공통 색상, API 엔드포인트 등의 전역 상태 명시적 분리

---

## Phase 1: 프로젝트 초기 설정

- [x] Flutter-Front 패키지 및 서버 폴더 구조화 완료
- [x] 불필요한 Spring-Back 하위 프로젝트(api501, springEx 등) 제거 후 `api5012` 폴더만 단일 유지
- [x] 개별 모듈 단위 `.git` 제거 및 최상위(`0-sample-flutter-projectt-k9`) 통합 `git init` 생성 완료
- [x] 노션 기획서 분석 완료
- [x] 기존 레포지토리 기준 전체 코드 구조 분석 완료

---

## Phase 2: Spring Boot 백엔드 (Spring-Back/SpringBasic/api5012)

### 2-1. 도메인 계층 (Entity + Enum)
- [x] MemberRole.java (USER, ADMIN 열거형)
- [x] BookStatus.java (AVAILABLE, RENTED, RESERVED, LOST)
- [x] RentalStatus.java (RENTING, RETURNED, OVERDUE, EXTENDED)
- [x] Member.java (회원 엔티티)
- [x] Book.java (도서 엔티티)
- [x] Rental.java (대여 엔티티)
- [x] Notice.java (공지사항 엔티티)
- [x] NoticeImage.java (공지 이미지 엔티티)
- [x] LibraryEvent.java (행사 엔티티)
- [x] EventApplication.java (행사 신청 엔티티)
- [x] Apply.java (시설 예약 엔티티)
- [x] WishBook.java (비치희망도서 엔티티)
- [x] Inquiry.java (문의사항 엔티티)
- [x] Reply.java (답변 엔티티)

### 2-2. Repository 계층
- [x] MemberRepository.java
- [x] BookRepository.java
- [x] RentalRepository.java
- [x] NoticeRepository.java
- [x] LibraryEventRepository.java
- [x] EventApplicationRepository.java
- [x] ApplyRepository.java
- [x] WishBookRepository.java
- [x] InquiryRepository.java
- [x] ReplyRepository.java

### 2-3. DTO 계층
- [x] MemberDTO.java / MemberSignupDTO.java / MemberLoginDTO.java
- [x] BookDTO.java
- [x] RentalDTO.java
- [x] NoticeDTO.java / NoticeImageDTO.java
- [x] LibraryEventDTO.java
- [x] EventApplicationDTO.java
- [x] ApplyDTO.java
- [x] WishBookDTO.java
- [x] InquiryDTO.java / ReplyDTO.java

### 2-4. Service 계층
- [x] MemberService / MemberServiceImpl
- [x] BookService / BookServiceImpl
- [x] RentalService / RentalServiceImpl
- [x] NoticeService / NoticeServiceImpl
- [x] EventService / EventServiceImpl
- [x] ApplyService / ApplyServiceImpl
- [x] WishBookService / WishBookServiceImpl
- [x] InquiryService / InquiryServiceImpl

### 2-5. Controller 계층 (REST API)
- [x] LibraryMemberController (/api/member/*)
- [x] BookController (/api/book/*)
- [x] RentalController (/api/rental/*)
- [x] NoticeController (/api/notice/*)
- [x] EventController (/api/event/*)
- [x] ApplyController (/api/apply/*)
- [x] WishBookController (/api/wishbook/*)
- [x] InquiryController (/api/inquiry/*)

### 2-6. 설정 파일
- [x] application.properties (MariaDB 설정 추가)
- [ ] Security 설정 업데이트 (회원가입 엔드포인트 허용)
- [ ] data.sql (초기 샘플 데이터)

---

## Phase 3: Flutter 프론트엔드 (Flutter-Front)

### 3-1. 기본 설정
- [x] pubspec.yaml 패키지 업데이트
- [x] const/api_constants.dart (API Base URL 관리)
- [x] const/app_colors.dart (앱 테마 색상)
- [x] main.dart (MultiProvider 등록)
- [x] my_app.dart (라우팅 설정)

### 3-2. Model 클래스
- [x] member_model.dart
- [x] book_model.dart
- [x] rental_model.dart
- [x] notice_model.dart
- [x] event_model.dart
- [x] inquiry_model.dart
- [x] apply_model.dart
- [x] wish_book_model.dart

### 3-3. Controller 클래스 (Provider)
- [x] login_controller.dart (기존 auth 폴더 위치 이용 확인)
- [x] signup_controller.dart (기존 auth 폴더 위치 이용 확인)
- [x] book_controller.dart (작업 내용: 도서 검색/목록 조회를 위한 `fetchBooks()` 비동기 처리 및 `ChangeNotifier` 연동 구현 완료 - 상세 한글 주석 작성)
- [x] rental_controller.dart (작업 내용: 도서 대여/반납 이력을 불러오는 `fetchMemberRentals()` 메서드 설계 및 로딩 상태 관리 지원 구현)
- [x] notice_controller.dart (작업 내용: 공지사항 페칭용 `fetchNotices()` 로직, 로딩 상태 관리 변수(`_isLoading`) 추가 및 코드 주석 보강)
- [x] event_controller.dart (작업 내용: 행사 리스트 조회용 Provider 설계 및 `LibraryEventModel`과 연동 완료)
- [x] inquiry_controller.dart (작업 내용: 1:1 문의 전송을 처리하는 `postInquiry()`의 비동기 반환(Boolean) 로직 포함)
- [x] reserve_controller.dart (작업 내용: 시설 예약/내역 조회를 위한 `fetchReservations()` 구성 및 `ApplyModel` 모델 적용)
- [x] ai_image_controller.dart (기존 ai 폴더 위치 판단)

### 3-4. Screen (화면)
- [x] splash_screen.dart (기존 my_splash 연동 확인)
- [x] main_screen.dart (기존 화면 연동 및 BottomNav 기반 확인)
- [x] login_screen.dart (기존 로그인 연동 유지)
- [x] signup_screen.dart (기존 회원가입 연동 유지)
- [x] mypage_screen.dart (작업: 마이페이지 ListView 메뉴 뼈대 완성, 회원정보/대여/문의/로그아웃 네비게이션 렌더링 주석 추가)
- [x] book/book_list_screen.dart (작업: 도서 검색용 리스트 UI, Consumer 패턴으로 로딩/빈 상태 관리 연동 완료)
- [x] book/book_detail_screen.dart (작업: 선택한 도서 ID 파라미터 매핑, 사진 영역 Placeholder 및 예약 버튼 완성)
- [x] event/event_list_screen.dart (작업: 이달의 도서관 행사 리스트 UI, EventController 비동기 조회 연결)
- [x] event/event_detail_screen.dart (작업: 특정 행사 디테일 및 참가 신청 프롬프트 화면 생성)
- [x] notice/notice_list_screen.dart (작업: ListTitle 기반 공지사항 최신 글 조회, NoticeController 연동 및 로더 추가)
- [x] notice/notice_detail_screen.dart (작업: 상세 공지내용 출력 화면 라우팅 연결 구성)
- [x] rental/rental_list_screen.dart (작업: 도서대여 상태 모니터링 카드 리스트, 날짜/상태 칩(Chip) 디자인 적용)
- [x] reserve/facility_reserve_screen.dart (작업: 도서관 스터디룸 예약 및 보유 현황 리스트, ReserveController 결합)
- [x] inquiry/inquiry_list_screen.dart (작업: 나의 1:1 문의사항 목록, 답변(Reply) 상태 표시기 및 플로팅 버튼 적용)
- [x] inquiry/inquiry_write_screen.dart (작업: 신규 문의용 글쓰기 폼, TextField 및 제출 버튼 SnackBar 메시지 등 완료)
- [x] ai/ai_image_screen.dart (기존 ai 폴더 위치 판단)

---

## Phase 4: Flask AI 서버 정리 (Flask-Back)

- [x] app.py 엔드포인트 정리 및 주석 추가 (작업: config 파일 임포트화 및 `/predict/<model_type>` 주석 추가 완료)
- [x] config.py 설정 정리 (작업: BASE_DIR 활용, animal/appliance/tool 딕셔너리 명칭 변경, 하드코딩 제거 완료)
- [x] CORS 설정 확인 (작업: `CORS(app)` 전역 적용 확인 및 주석 추가 완료)
- [x] /predict/tool, /predict/animal, /predict/appliance 엔드포인트 확인 (작업: config.py의 VISION_MODEL_CONFIGS 변경으로 파라미터 매핑 통합 완료)
- [x] requirements.txt 정리 (작업: 미사용 FastAPI 종속성 제거 및 pandas, waitress 등 필수 패키지명 기입 완료)

---

## Phase 5: 통합 및 리팩토링

- [x] 전체 코드 주석 점검 (한글 주석) (기존 화면 개발 단계에서 상세 보완 완료)
- [x] 코드 리팩토링 및 정리 (앱, 라우팅, 컨트롤러 최적화 및 Flask 코드 컨벤션 재배치 완료)
- [x] README.md 작성 (Notion 기획서 기반으로 통합 문서 구성 및 Github 동기화/Push 완료)
- [x] API 문서 정리 (프로젝트 최상단에 API_DOCS.md 파일을 도출하여 모든 기능 엔드포인트 명세서 등록 완료)

---

## 작업 로그

| 날짜 | 세션 | 완료 항목 | 비고 |
|------|------|----------|------|
| 2026-03-27 | 1차 | Phase 1 전체, Phase 2 (2-1 ~ 2-5) | Spring Boot 백엔드 도메인~컨트롤러 완료 (37+16 = 53개 파일) |
| 2026-03-27 | 2차 | Phase 3-1 | Flutter 프론트엔드 기본 설정 완료 (상수, Color, my_app.dart 등) 및 단위(const)테스트 통과 |
| 2026-03-27 | 2차 | Phase 3-2 | 8개 주요 Model 클래스 (member, book 등) 작성 및 단위(model)테스트 통과 |
| 2026-03-27 | 2차 | Phase 3-3 | 6개 신규 Controller 클래스 생성 완료 (Provider 상태 관리 및 한글 비동기 주석 추가) |
| 2026-03-27 | 2차 | Phase 3-4 | 도서, 로그인, 렌탈, 예약, 마이페이지 등 총 11개의 Screen UI 컴포넌트 개발 및 기능 주석 문서화 완료 |
| 2026-03-27 | 2차 | Phase 4 | Flask-Back 서버 내 config.py 모델 분리 정리, app.py 코드 개선, 미사용 라이브러리 정리 및 한글 커밋 |
| 2026-03-27 | 2차 | Phase 5 | 통합 API_DOCS.md 문서 수립, 주석 일제 점검 및 체크리스트 마감 완료 |
| | | | **개발 진행 상태: 전체 기초 구조 완성 및 Phase 완료! 🎉** |

---

## 이어서 작업하기 (Resume Guide)

다음 세션에서 이어서 작업할 때:
1. 이 CHECKLIST.md를 먼저 읽어주세요
2. 체크되지 않은 항목부터 순서대로 진행
3. 현재 다음 작업: **Phase 3 - Flutter 프론트엔드 구축**
4. 명령어: "CHECKLIST.md 보고 이어서 작업해줘"
