# FEATURE_FILES.md — 프로젝트 기능 파일 현황 및 작업 체크리스트

> 최종 수정: 2026-04-10
> 프로젝트: Flutter + Spring Boot 도서관 통합 앱
> 구성: Flutter 프론트엔드 + Spring Boot 백엔드 (JWT 인증 + 도서관 API)

---

## 목차

1. [전체 아키텍처 개요](#1-전체-아키텍처-개요)
2. [회원 시스템 구조](#2-회원-시스템-구조)
3. [Flutter 화면 구조 (탭 기반)](#3-flutter-화면-구조-탭-기반)
4. [Flutter 파일 현황](#4-flutter-파일-현황)
5. [Spring 백엔드 파일 현황](#5-spring-백엔드-파일-현황)
6. [완료된 작업 체크리스트](#6-완료된-작업-체크리스트)
7. [현재 동작 흐름](#7-현재-동작-흐름)
8. [남은 작업 및 개선 사항](#8-남은-작업-및-개선-사항)
9. [API 엔드포인트 매핑표](#9-api-엔드포인트-매핑표)

---

## 1. 전체 아키텍처 개요

```
Flutter App (Android Emulator)
        |
        | HTTP (10.0.2.2:8080)
        v
Spring Boot 서버 (localhost:8080)
   +-- JWT 인증 시스템  (/member/*, /generateToken)
   |      +-- APIUser 테이블
   +-- 도서관 API 시스템  (/api/*)
          +-- LibraryMember 테이블
              +-- Book, Rental, Notice, Event
              +-- Inquiry, Apply (시설예약)
              +-- WishBook, Reply
```

**주요 상수 (Flutter-Front/lib/const/api_constants.dart):**
- Spring API base: `http://10.0.2.2:8080/api`
- JWT base: `http://10.0.2.2:8080`
- Flask AI base: `http://10.0.2.2:5000/predict`

---

## 2. 회원 시스템 구조

> **핵심**: Spring에는 두 개의 독립적인 회원 테이블이 존재합니다.

| 구분 | 엔티티 | 용도 | 가입 엔드포인트 |
|------|--------|------|----------------|
| JWT 인증 | `APIUser` | JWT 토큰 발급 | `POST /member/register` |
| 도서관 | `Member` (LibraryMember) | 도서관 모든 API | `POST /api/member/signup` |

**Flutter 가입 시 두 엔드포인트 모두 호출 (signup_controller.dart):**
1. `POST /member/register` → APIUser 생성 (JWT 로그인용)
2. `POST /api/member/signup` → LibraryMember 생성 (도서관 API용, memberId 확보)

**FlutterSecureStorage 저장 키:**

| 키 | 값 | 용도 |
|----|-----|------|
| `accessToken` | JWT Bearer 토큰 | API 요청 인증 헤더 |
| `refreshToken` | JWT 리프레시 토큰 | 토큰 갱신 |
| `mid` | String (사용자 아이디) | 사용자 식별 |
| `memberId` | Long (DB 기본키) | 도서관 API 파라미터 |
| `profileImg` | 프로필 이미지 경로 | UI 표시 |

---

## 3. Flutter 화면 구조 (탭 기반)

### 앱 진입 흐름
```
MySplash2 (3초)
    |
    v
MainTabScreen  <-- 모든 진입점 (/main 라우트)
    |
    +-- [0] HomeTab       : 비로그인/로그인 공통
    +-- [1] BookTab       : 로그인 필요
    +-- [2] MyServiceTab  : 로그인 필요
    +-- [3] AiTab         : 로그인 필요
    +-- [4] MyPageTab     : 로그인 필요
```

### 탭별 구조

| 탭 | 화면명 | 주요 기능 |
|----|--------|----------|
| 0 홈 | HomeTab | PageView 배너(자동슬라이드) + 공지사항 미리보기 3건 + 이벤트 미리보기 3건 |
| 1 도서 | BookTab | 검색바(로컬필터) + 도서 ListView → /bookDetail |
| 2 내 서비스 | MyServiceTab | 중첩 TabBar: 대여현황 / 1:1문의(FAB) / 시설예약(폼+목록) |
| 3 AI | AiTab | AI이미지 카드 → /ai-image / AI주가 카드 → /ai-stock |
| 4 마이페이지 | MyPageTab | 회원정보(이름/이메일/지역/가입일) + 바로가기 + 로그아웃 |

### 공통 위젯 (lib/widget/common/)

| 파일 | 용도 |
|------|------|
| `app_base_layout.dart` | 공통 Scaffold 래퍼 + LoginRequiredWidget |
| `loading_widget.dart` | 로딩 스피너 (메시지 선택) |
| `empty_widget.dart` | 빈 결과 (아이콘 + 메시지) |
| `section_header.dart` | 섹션 타이틀 + 더보기 버튼 |
| `library_card_tile.dart` | 카드형 ListTile (아이콘/제목/부제목/트레일링) |

---

## 4. Flutter 파일 현황

### 4-1. 앱 진입점

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/main.dart | 완료 | MultiProvider 등록 (11개 컨트롤러) |
| Flutter-Front/lib/my_app.dart | 완료 | Material3 테마 + 전체 Named Route 등록 |
| Flutter-Front/lib/screen/my_splash2.dart | 완료 | 스플래시 → /main 이동 |
| Flutter-Front/lib/const/api_constants.dart | 완료 | API base URL 상수 |

### 4-2. 탭 메인 화면 (신규)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/screen/tab/main_tab_screen.dart | 완료 | BottomNavigationBar(NavigationBar) 5탭, IndexedStack 상태보존 |
| Flutter-Front/lib/screen/tab/home_tab.dart | 완료 | PageView 배너(Timer 자동슬라이드) + 공지/이벤트 미리보기 |
| Flutter-Front/lib/screen/tab/book_tab.dart | 완료 | 검색바 + 도서 ListView (상태 기반 색상 Chip) |
| Flutter-Front/lib/screen/tab/my_service_tab.dart | 완료 | 중첩 TabController(대여/문의/예약) + FAB 문의작성 |
| Flutter-Front/lib/screen/tab/ai_tab.dart | 완료 | AI기능 선택 카드 2종 |
| Flutter-Front/lib/screen/tab/mypage_tab.dart | 완료 | 회원정보 카드(mname/email/region/regDate) + 메뉴 |

### 4-3. 공통 위젯 (신규)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/widget/common/app_base_layout.dart | 완료 | 베이스 Scaffold + LoginRequiredWidget |
| Flutter-Front/lib/widget/common/loading_widget.dart | 완료 | 로딩 공통 위젯 |
| Flutter-Front/lib/widget/common/empty_widget.dart | 완료 | 빈 결과 공통 위젯 |
| Flutter-Front/lib/widget/common/section_header.dart | 완료 | 섹션 헤더(타이틀+더보기) |
| Flutter-Front/lib/widget/common/library_card_tile.dart | 완료 | 카드형 ListTile 공통 위젯 |

### 4-4. 인증 (Auth)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/auth/login_controller.dart | 완료 | JWT 로그인, loadMemberInfo(), 회원정보 메모리 저장, 로그아웃 |
| Flutter-Front/lib/controller/auth/signup_controller.dart | 완료 | 이중 가입 (APIUser + LibraryMember), mname/region 필드 |
| Flutter-Front/lib/screen/login_screen.dart | 완료 | 로그인 화면 |
| Flutter-Front/lib/screen/signup_screen.dart | 완료 | 가입 화면 (아이디/이름/이메일/지역/비밀번호) |

### 4-5. 도서 (Book)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/book_controller.dart | 완료 | fetchBooks(), fetchBookById() |
| Flutter-Front/lib/model/book_model.dart | 완료 | id, title, author, publisher, isbn, status |
| Flutter-Front/lib/screen/book/book_list_screen.dart | 완료 | 독립 화면 (Navigator.push 대상) |
| Flutter-Front/lib/screen/book/book_detail_screen.dart | 완료 | 실제 API 조회 + 대여 신청 버튼 |

### 4-6. 대여 (Rental)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/rental_controller.dart | 완료 | fetchMemberRentals(), rentBook(bookId) |
| Flutter-Front/lib/model/rental_model.dart | 완료 | bookId, rentDate, returnDate, status |
| Flutter-Front/lib/screen/rental/rental_list_screen.dart | 완료 | 독립 화면 |

### 4-7. 공지사항 (Notice)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/notice_controller.dart | 완료 | fetchNotices() |
| Flutter-Front/lib/model/notice_model.dart | 완료 | id, title, regDate, viewCount |
| Flutter-Front/lib/screen/notice/notice_list_screen.dart | 완료 | 독립 화면 |
| Flutter-Front/lib/screen/notice/notice_detail_screen.dart | 완료 | 독립 화면 |

### 4-8. 이벤트 (Event)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/event_controller.dart | 완료 | fetchEvents(), applyEvent() |
| Flutter-Front/lib/model/event_model.dart | 완료 | startDate\|\|eventDate fallback |
| Flutter-Front/lib/screen/event/event_list_screen.dart | 완료 | 독립 화면 |
| Flutter-Front/lib/screen/event/event_detail_screen.dart | 완료 | 독립 화면 |

### 4-9. 1:1 문의 (Inquiry)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/inquiry_controller.dart | 완료 | fetchMyInquiries(), postInquiry() |
| Flutter-Front/lib/model/inquiry_model.dart | 완료 | answered\|\|isReplied fallback |
| Flutter-Front/lib/screen/inquiry/inquiry_list_screen.dart | 완료 | 독립 화면 |
| Flutter-Front/lib/screen/inquiry/inquiry_write_screen.dart | 완료 | 문의 작성 (StatefulWidget) |

### 4-10. 시설 예약 (Reserve)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/reserve_controller.dart | 완료 | fetchReservations(), createReservation() |
| Flutter-Front/lib/model/apply_model.dart | 완료 | facilityType\|\|facilityName, reserveDate\|\|applyDate fallback |
| Flutter-Front/lib/screen/reserve/facility_reserve_screen.dart | 완료 | 독립 화면 (폼 + 목록) |

### 4-11. AI 기능

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/ai/image/ai_image_controller.dart | 완료 | Flask AI 이미지 분석 |
| Flutter-Front/lib/controller/ai/stock/ai_stock_provider.dart | 완료 | AI 주가 예측 |
| Flutter-Front/lib/screen/ai/image/ai_image_screen.dart | 완료 | AI 이미지 화면 |
| Flutter-Front/lib/screen/ai/stock/ai_stock_screen.dart | 완료 | AI 주가 화면 |

---

## 5. Spring 백엔드 파일 현황

### JWT 인증
| 파일 | 설명 |
|------|------|
| security/filter/APILoginFilter.java | POST /generateToken → JWT 발급 |
| security/handler/APILoginSuccessHandler.java | accessToken + refreshToken 응답 |
| security/filter/TokenCheckFilter.java | Bearer 토큰 검증 |
| service/MemberServiceImpl.java | POST /member/register → APIUser 생성 |

### 도서관 회원
| 파일 | 설명 |
|------|------|
| controller/library/LibraryMemberController.java | POST /api/member/signup, GET /api/member/me |
| service/library/MemberLibraryServiceImpl.java | LibraryMember CRUD, BCrypt 암호화 |
| domain/library/Member.java | mid, mpw, mname, email, region, role |

### 도서관 기능 API
| 컨트롤러 | 주요 엔드포인트 |
|----------|---------------|
| BookController.java | GET /api/book?page&size, GET /api/book/{id} |
| RentalController.java | GET /api/rental?memberId&page&size, POST /api/rental |
| NoticeController.java | GET /api/notice?page&size, GET /api/notice/{id} |
| EventController.java | GET /api/event?page&size, POST /api/event/{id}/apply?memberId |
| InquiryController.java | GET /api/inquiry/my?memberId, POST /api/inquiry?memberId |
| ApplyController.java | GET /api/apply/my?memberId&page&size, POST /api/apply?memberId |

---

## 6. 완료된 작업 체크리스트

### 탭 기반 레이아웃 개편 (이번 세션)
- [x] `MainTabScreen` — NavigationBar 5탭, IndexedStack 상태보존, 로그인 가드
- [x] `HomeTab` — PageView 배너(Timer 자동슬라이드 3초) + 공지/이벤트 미리보기 + RefreshIndicator
- [x] `BookTab` — TextField 검색바(로컬필터) + ListView + 상태 Chip
- [x] `MyServiceTab` — 중첩 TabController (대여현황 / 1:1문의+FAB / 시설예약폼)
- [x] `AiTab` — AI 기능 카드 2종 (이미지 분석 / 주가 예측)
- [x] `MyPageTab` — 회원정보 카드(mname/email/region/regDate) + 바로가기 메뉴

### 공통 위젯 분리
- [x] `AppBaseLayout` — 공통 Scaffold + `LoginRequiredWidget`
- [x] `LoadingWidget` — 메시지 선택 가능한 로딩 스피너
- [x] `EmptyWidget` — 아이콘 + 메시지 빈 결과 위젯
- [x] `SectionHeader` — 타이틀 + 더보기 버튼
- [x] `LibraryCardTile` — Card 기반 공통 ListTile

### LoginController 개선
- [x] `loadMemberInfo()` — GET /api/member/me 호출, 회원정보 메모리 저장
- [x] `isMemberInfoLoading` 로딩 상태 추가
- [x] `currentMid`, `memberName`, `memberEmail`, `memberRegion`, `memberRegDate` 노출
- [x] 로그아웃 시 모든 회원정보 초기화

### 이전 세션 완료 항목
- [x] 회원가입 이중 연동 (APIUser + LibraryMember)
- [x] mname/region 가입 폼 필드 추가
- [x] 5개 컨트롤러 실제 API 연동 (Rental/Notice/Event/Inquiry/Reserve)
- [x] 도서 대여 신청 기능 (rentBook)
- [x] 시설 예약 폼 UI
- [x] 모델 필드 불일치 수정 (??fallback)
- [x] RenderFlex 오버플로 수정

---

## 7. 현재 동작 흐름

### 앱 진입 ~ 메인화면
```
MySplash2 (3초 대기)
    |
    +-- Navigator.pushReplacementNamed('/main')
    |
MainTabScreen
    +-- [홈 탭] 비로그인: 로그인/가입 버튼 표시
    |           로그인: 공지/이벤트 미리보기
    +-- [기타 탭] 비로그인 → 로그인 유도 다이얼로그
```

### 회원가입 플로우
```
SignupScreen (아이디/이름/이메일/지역/비밀번호)
    +-- POST /member/register    → APIUser 생성
    +-- POST /api/member/signup  → LibraryMember 생성
    +-- 성공 → /login
```

### 로그인 플로우
```
LoginScreen
    +-- POST /generateToken      → accessToken/refreshToken 저장
    +-- GET  /api/member/me      → memberId + 회원정보 저장
    +-- 성공 → /main (MainTabScreen)
```

### 도서관 API 공통 패턴
```dart
Authorization: Bearer $accessToken   // 모든 API 요청
memberId: $memberId                   // 회원 관련 파라미터
```

---

## 8. 남은 작업 및 개선 사항

### 필수

- [ ] **JWT 토큰 만료 처리**
  401 응답 시 RefreshToken으로 자동 갱신 또는 /login 이동.
  현재: 401 응답 시 빈 목록 또는 에러 메시지만 표시

- [ ] **이중 가입 실패 롤백**
  Step1(APIUser) 성공 후 Step2(LibraryMember) 실패 시 불완전 상태 처리

- [ ] **도서 대여 신청 API 경로 확인**
  Spring `RentalController`에서 `POST /api/rental?memberId&bookId` 파라미터 방식 확인
  (현재 Flutter: query param 방식 — Spring이 body 방식이면 수정 필요)

### 중요 (UX)

- [ ] **공지사항/이벤트 페이지네이션**
  현재 첫 20건만 조회. 무한 스크롤 또는 더보기 구현

- [ ] **대여 현황에 도서 제목 표시**
  현재 `도서 #123` 형태 → bookId로 도서 정보 조회 후 제목 표시

- [ ] **홈탭 배너 이미지 교체**
  현재 컬러 그라디언트 + 아이콘. 실제 도서관 이미지로 교체 가능

- [ ] **도서 목록 API 경로 확인**
  현재 `GET /api/book?page&size`. Spring이 `/api/book/list` 형태면 수정

### 선택

- [ ] **희망 도서 신청** — Spring WishBook 도메인 존재, Flutter 화면 미구현
- [ ] **문의 답변 상세 조회** — Reply 도메인 활용
- [ ] **앱 테마 커스텀** — Material3 ColorScheme 브랜딩
- [ ] **오프라인 캐싱** — 마지막 조회 데이터 로컬 저장

---

## 9. API 엔드포인트 매핑표

| 기능 | 메서드 | 엔드포인트 | Flutter 파일 | JWT |
|------|--------|-----------|-------------|-----|
| APIUser 가입 | POST | `/member/register` | signup_controller.dart | 없음 |
| 로그인 (JWT) | POST | `/generateToken` | login_controller.dart | 없음 |
| LibraryMember 가입 | POST | `/api/member/signup` | signup_controller.dart | 없음 |
| 내 회원정보 | GET | `/api/member/me?mid={}` | login_controller.dart | JWT |
| 도서 목록 | GET | `/api/book?page&size` | book_controller.dart | JWT |
| 도서 상세 | GET | `/api/book/{id}` | book_controller.dart | JWT |
| 도서 대여 신청 | POST | `/api/rental?memberId&bookId` | rental_controller.dart | JWT |
| 내 대여 목록 | GET | `/api/rental?memberId&page&size` | rental_controller.dart | JWT |
| 공지사항 목록 | GET | `/api/notice?page&size` | notice_controller.dart | 선택 |
| 공지사항 상세 | GET | `/api/notice/{id}` | notice_controller.dart | 선택 |
| 이벤트 목록 | GET | `/api/event?page&size` | event_controller.dart | 선택 |
| 이벤트 신청 | POST | `/api/event/{id}/apply?memberId` | event_controller.dart | JWT |
| 내 문의 목록 | GET | `/api/inquiry/my?memberId` | inquiry_controller.dart | JWT |
| 문의 작성 | POST | `/api/inquiry?memberId` | inquiry_controller.dart | JWT |
| 내 예약 목록 | GET | `/api/apply/my?memberId&page&size` | reserve_controller.dart | JWT |
| 시설 예약 신청 | POST | `/api/apply?memberId` | reserve_controller.dart | JWT |
