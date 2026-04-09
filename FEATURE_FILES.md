# FEATURE_FILES.md — 프로젝트 기능 파일 현황 및 작업 체크리스트

> 작성일: 2026-04-09
> 프로젝트: Flutter + Spring Boot 도서관 통합 앱
> 구성: Flutter 프론트엔드 + Spring Boot 백엔드 (JWT 인증 + 도서관 API)

---

## 목차

1. [전체 아키텍처 개요](#1-전체-아키텍처-개요)
2. [회원 시스템 구조](#2-회원-시스템-구조)
3. [Flutter 프론트엔드 파일 현황](#3-flutter-프론트엔드-파일-현황)
4. [Spring 백엔드 파일 현황](#4-spring-백엔드-파일-현황)
5. [완료된 작업 체크리스트](#5-완료된-작업-체크리스트)
6. [현재 동작 흐름](#6-현재-동작-흐름)
7. [남은 작업 및 개선 사항](#7-남은-작업-및-개선-사항)
8. [API 엔드포인트 매핑표](#8-api-엔드포인트-매핑표)

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
- Flutter API base: `http://10.0.2.2:8080/api` (`ApiConstants.springBaseUrl`)
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
2. `POST /api/member/signup` → LibraryMember 생성 (도서관 API용, `memberId` 확보)

**로그인 후 FlutterSecureStorage 저장 키:**

| 키 | 값 | 용도 |
|----|-----|------|
| `accessToken` | JWT Bearer 토큰 | API 요청 인증 헤더 |
| `refreshToken` | JWT 리프레시 토큰 | 토큰 갱신 |
| `mid` | String (사용자 아이디) | 사용자 식별 |
| `memberId` | Long (DB 기본키) | 도서관 API 파라미터 |
| `profileImg` | 프로필 이미지 경로 | UI 표시 |

---

## 3. Flutter 프론트엔드 파일 현황

### 3-1. 진입점 및 라우팅

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/main.dart | 완료 | MultiProvider 등록 (7개 컨트롤러), 앱 진입점 |
| Flutter-Front/lib/my_app.dart | 완료 | 전체 Named Route 등록 (13개 라우트) |
| Flutter-Front/lib/screen/my_app_routing.dart | 완료 | 라우팅 설정 파일 |
| Flutter-Front/lib/const/api_constants.dart | 완료 | API base URL 상수 정의 |

**등록된 Named 라우트:**
```
/main, /login, /signup, /mypage
/bookList, /bookDetail
/rentalList
/noticeList, /noticeDetail
/eventList, /eventDetail
/inquiryList, /inquiryWrite
/facilityReserve
/ai-image, /ai-stock
```

### 3-2. 인증 (Auth)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/auth/login_controller.dart | 완료 | JWT 로그인, memberId 조회/저장, 로그아웃 |
| Flutter-Front/lib/controller/auth/signup_controller.dart | 완료 | 이중 가입 (APIUser + LibraryMember), mname/region 필드 |
| Flutter-Front/lib/screen/login_screen.dart | 완료 | 로그인 화면 |
| Flutter-Front/lib/screen/signup_screen.dart | 완료 | 회원가입 화면 (아이디/이름/이메일/지역/비밀번호) |

### 3-3. 메인/마이페이지

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/screen/main_screen.dart | 완료 | 로그인 여부에 따른 UI 분기, 도서관 메뉴 버튼 |
| Flutter-Front/lib/screen/mypage_screen.dart | 완료 | 마이페이지, 로그아웃 (JWT 전체 삭제) |
| Flutter-Front/lib/screen/my_splash2.dart | 완료 | 스플래시 화면 (RenderFlex 오버플로 수정됨) |

### 3-4. 도서 (Book)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/book_controller.dart | 완료 | 도서 목록/검색, JWT 헤더 포함 |
| Flutter-Front/lib/model/book_model.dart | 완료 | BookModel (fromJson) |
| Flutter-Front/lib/screen/book/book_list_screen.dart | 완료 | 도서 목록 화면 |
| Flutter-Front/lib/screen/book/book_detail_screen.dart | 완료 | 도서 상세 화면 |

### 3-5. 대여 (Rental)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/rental_controller.dart | 완료 | `GET /api/rental?memberId` Page 파싱, JWT 인증 |
| Flutter-Front/lib/model/rental_model.dart | 완료 | RentalModel (bookId, rentDate, returnDate, status) |
| Flutter-Front/lib/screen/rental/rental_list_screen.dart | 완료 | 대여 현황 목록 화면 |

### 3-6. 공지사항 (Notice)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/notice_controller.dart | 완료 | `GET /api/notice?page&size` |
| Flutter-Front/lib/model/notice_model.dart | 완료 | NoticeModel |
| Flutter-Front/lib/screen/notice/notice_list_screen.dart | 완료 | 공지사항 목록 화면 |
| Flutter-Front/lib/screen/notice/notice_detail_screen.dart | 완료 | 공지사항 상세 화면 |

### 3-7. 이벤트 (Event)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/event_controller.dart | 완료 | 이벤트 목록 조회 + `applyEvent()` 신청 |
| Flutter-Front/lib/model/event_model.dart | 완료 | `startDate \|\| eventDate` fallback 처리 |
| Flutter-Front/lib/screen/event/event_list_screen.dart | 완료 | 이벤트 목록 화면 |
| Flutter-Front/lib/screen/event/event_detail_screen.dart | 완료 | 이벤트 상세 및 신청 화면 |

### 3-8. 1:1 문의 (Inquiry)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/inquiry_controller.dart | 완료 | 내 문의 목록, 문의 작성 |
| Flutter-Front/lib/model/inquiry_model.dart | 완료 | `answered \|\| isReplied` fallback 처리 |
| Flutter-Front/lib/screen/inquiry/inquiry_list_screen.dart | 완료 | 내 문의 목록 화면 |
| Flutter-Front/lib/screen/inquiry/inquiry_write_screen.dart | 완료 | 문의 작성 화면 (제목/내용 입력, API 연동) |

### 3-9. 시설 예약 (Reserve/Apply)

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/reserve_controller.dart | 완료 | 내 예약 목록 조회, 예약 신청 |
| Flutter-Front/lib/model/apply_model.dart | 완료 | `facilityType \|\| facilityName`, `reserveDate \|\| applyDate` fallback |
| Flutter-Front/lib/screen/reserve/facility_reserve_screen.dart | 완료 | 시설 예약 화면 |

### 3-10. AI 기능

| 파일 | 상태 | 설명 |
|------|------|------|
| Flutter-Front/lib/controller/ai/image/ai_image_controller.dart | 완료 | AI 이미지 분석 (Flask 연동) |
| Flutter-Front/lib/controller/ai/stock/ai_stock_provider.dart | 완료 | AI 삼성 주가 예측 |
| Flutter-Front/lib/screen/ai/image/ai_image_screen.dart | 완료 | AI 이미지 화면 |
| Flutter-Front/lib/screen/ai/stock/ai_stock_screen.dart | 완료 | AI 주가 예측 화면 |

---

## 4. Spring 백엔드 파일 현황

### 4-1. JWT 인증 시스템

| 파일 | 설명 |
|------|------|
| security/filter/APILoginFilter.java | `POST /generateToken` 처리, JWT 발급 |
| security/handler/APILoginSuccessHandler.java | accessToken + refreshToken JSON 응답 |
| security/filter/TokenCheckFilter.java | Bearer 토큰 검증 필터 |
| security/filter/RefreshTokenFilter.java | 리프레시 토큰 갱신 |
| domain/APIUser.java | JWT 인증용 회원 엔티티 |
| service/MemberServiceImpl.java | `POST /member/register` — APIUser 생성 |

### 4-2. 도서관 회원

| 파일 | 설명 |
|------|------|
| controller/library/LibraryMemberController.java | `POST /api/member/signup`, `GET /api/member/me` |
| service/library/MemberLibraryServiceImpl.java | LibraryMember CRUD, BCrypt 암호화, 중복 체크 |
| domain/library/Member.java | LibraryMember 엔티티 (mid, mpw, mname, email, region, role) |
| dto/library/MemberSignupDTO.java | 가입 DTO (mid, mpw, mpwConfirm, mname, email, region) |

### 4-3. 도서관 기능 API

| 컨트롤러 | 주요 엔드포인트 |
|----------|---------------|
| BookController.java | `GET /api/book?page&size`, `GET /api/book/{id}` |
| RentalController.java | `GET /api/rental?memberId&page&size` |
| NoticeController.java | `GET /api/notice?page&size`, `GET /api/notice/{id}` |
| EventController.java | `GET /api/event?page&size`, `POST /api/event/{id}/apply?memberId` |
| InquiryController.java | `GET /api/inquiry/my?memberId`, `POST /api/inquiry?memberId` |
| ApplyController.java | `GET /api/apply/my?memberId&page&size`, `POST /api/apply?memberId` |

---

## 5. 완료된 작업 체크리스트

### 버그 수정
- [x] `my_splash2.dart` RenderFlex 오버플로 수정 (Image 고정 width 제거, Column을 Expanded로 래핑)
- [x] 회원가입 400 오류 수정 (multipart/form-data → JSON body, Spring `@RequestBody` 맞춤)
- [x] `Expanded` 닫는 괄호 누락 수정

### 인증 연동
- [x] 로그인 후 `memberId` (LibraryMember numeric ID) 조회 및 SecureStorage 저장
- [x] 로그아웃 시 모든 SecureStorage 키 삭제 (`memberId` 포함)
- [x] 마이페이지 로그아웃 → `loginController.showLogoutDialog()` 연결 (기존: 단순 navigate)
- [x] 모든 async 이후 `context.mounted` 안전 체크 추가

### 회원가입 통합 연동 (이번 세션 핵심)
- [x] `signup_controller.dart` 이중 가입 구현
  - Step 1: `POST /member/register` (APIUser — JWT 로그인용)
  - Step 2: `POST /api/member/signup` (LibraryMember — 도서관 API용)
- [x] 가입 폼 필드 추가: `mname` (이름, 필수), `region` (지역, 선택)
- [x] `mpwConfirm` 필드 포함하여 Spring 서버 검증 통과
- [x] `isLoading` 상태로 가입 버튼 중복 클릭 방지 + 스피너 표시
- [x] 가입 성공 시 `/login` 화면으로 이동
- [x] 불필요한 이미지 피커 코드 제거 (signup_controller, signup_screen)
- [x] `signup_screen.dart` 비밀번호 일치 여부 텍스트 표시 추가

### Provider/라우팅 설정
- [x] `main.dart` MultiProvider에 6개 도서관 컨트롤러 등록
- [x] `my_app.dart` 전체 도서관 라우트 13개 등록
- [x] 불필요한 sample_design 임포트 제거

### 컨트롤러 API 연동 (스텁 → 실제 API)
- [x] `RentalController` — `GET /api/rental?memberId` + Spring Page 파싱
- [x] `NoticeController` — `GET /api/notice?page&size`
- [x] `EventController` — 목록 조회 + `applyEvent()` 신청
- [x] `InquiryController` — 내 목록 조회 + `postInquiry()`
- [x] `ReserveController` — 내 예약 목록 + `createReservation()`

### 모델 필드 불일치 수정
- [x] `EventModel` — `startDate || eventDate` fallback
- [x] `InquiryModel` — `answered || isReplied` fallback
- [x] `ApplyModel` — `facilityType || facilityName`, `reserveDate || applyDate` fallback

### 화면 수정
- [x] `inquiry_write_screen.dart` — StatefulWidget으로 변환, 실제 API 연동
- [x] `rental_list_screen.dart` — 하드코딩된 memberId 제거

---

## 6. 현재 동작 흐름

### 회원가입 플로우
```
SignupScreen 입력 (아이디 / 이름 / 이메일 / 지역 / 비밀번호)
    |
    +-- POST /member/register   --> APIUser 생성 (JWT 인증용)
    |       실패시 -> 에러 메시지 표시, 중단
    |
    +-- POST /api/member/signup --> LibraryMember 생성 (도서관 API용)
            성공 -> /login 화면 이동
            실패 -> 에러 메시지 표시
```

### 로그인 플로우
```
LoginScreen 입력 (아이디 / 비밀번호)
    |
    +-- POST /generateToken
    |       --> accessToken, refreshToken, profileImg 저장
    |       --> mid 저장
    |
    +-- GET /api/member/me?mid={}
            --> memberId (Long) 저장
            --> MainScreen 이동 (도서관 메뉴 표시)
```

### 도서관 API 호출 패턴 (모든 컨트롤러 공통)
```dart
final accessToken = await _secureStorage.read(key: "accessToken");
final memberId    = await _secureStorage.read(key: "memberId");

http.get(url, headers: {
  'Authorization': 'Bearer $accessToken',
  'Content-Type': 'application/json',
});
```

---

## 7. 남은 작업 및 개선 사항

### 필수 (기능 완성)

- [ ] **`/api/member/me` 엔드포인트 실존 확인**
  Spring `LibraryMemberController.java`에 `GET /api/member/me?mid={}` 구현 여부 검증
  (로그인 후 memberId 저장의 핵심 — 미구현 시 모든 도서관 API 실패)

- [ ] **도서 대여 신청 기능 구현**
  `book_detail_screen.dart`에 대여 신청 버튼 → `POST /api/rental?memberId={}` 연동

- [ ] **시설 예약 폼 UI 완성**
  `facility_reserve_screen.dart` 예약 입력 폼 (facilityType, reserveDate, applicantName, phone, participants)
  `ReserveController.createReservation()` 호출 연결

- [ ] **이벤트 신청 버튼 연동 확인**
  `event_detail_screen.dart`에서 `EventController.applyEvent()` 실제 호출 여부 확인

### 중요 (UX/안정성)

- [ ] **JWT 토큰 만료 처리**
  401 응답 시 RefreshToken으로 자동 갱신 또는 로그인 화면으로 이동

- [ ] **이중 가입 실패 롤백 처리**
  Step 1(APIUser) 성공 후 Step 2(LibraryMember) 실패 시 — 현재는 에러 메시지만 표시
  (불완전 상태: APIUser는 생성되었으나 LibraryMember 없음)

- [ ] **공지/이벤트 페이지네이션**
  현재 첫 20건만 조회. 무한 스크롤 또는 더보기 버튼 구현

- [ ] **마이페이지 실제 회원 정보 표시**
  `GET /api/member/me?mid={}` 결과로 이름, 이메일, 지역 표시 (현재: mid만 표시)

- [ ] **도서 검색 UI 연동**
  `book_list_screen.dart`에 검색어 입력 TextField → `BookController` 검색 메서드 연결

### 선택 (추가 기능)

- [ ] **희망 도서 신청** — Spring `WishBook` 도메인/서비스/컨트롤러 존재, Flutter 화면 미구현
- [ ] **문의 답변 상세 조회** — `Reply` 도메인 활용, 문의 상세 화면 추가
- [ ] **대여 이력 상세** — 반납 날짜, 연체 여부 표시
- [ ] **앱 아이콘 및 스플래시 브랜딩** — `my_splash2.dart` 완성

---

## 8. API 엔드포인트 매핑표

| 기능 | 메서드 | 엔드포인트 | Flutter 컨트롤러 | JWT 필요 |
|------|--------|-----------|-----------------|---------|
| APIUser 가입 | POST | `/member/register` | signup_controller.dart | 없음 |
| 로그인 (JWT 발급) | POST | `/generateToken` | login_controller.dart | 없음 |
| LibraryMember 가입 | POST | `/api/member/signup` | signup_controller.dart | 없음 |
| 내 회원정보 조회 | GET | `/api/member/me?mid={}` | login_controller.dart | JWT |
| 도서 목록 | GET | `/api/book?page&size` | book_controller.dart | JWT |
| 도서 상세 | GET | `/api/book/{id}` | book_controller.dart | JWT |
| 내 대여 목록 | GET | `/api/rental?memberId&page&size` | rental_controller.dart | JWT |
| 공지사항 목록 | GET | `/api/notice?page&size` | notice_controller.dart | 선택 |
| 공지사항 상세 | GET | `/api/notice/{id}` | notice_controller.dart | 선택 |
| 이벤트 목록 | GET | `/api/event?page&size` | event_controller.dart | 선택 |
| 이벤트 신청 | POST | `/api/event/{id}/apply?memberId` | event_controller.dart | JWT |
| 내 문의 목록 | GET | `/api/inquiry/my?memberId` | inquiry_controller.dart | JWT |
| 문의 작성 | POST | `/api/inquiry?memberId` | inquiry_controller.dart | JWT |
| 내 예약 목록 | GET | `/api/apply/my?memberId&page&size` | reserve_controller.dart | JWT |
| 시설 예약 신청 | POST | `/api/apply?memberId` | reserve_controller.dart | JWT |
