# 📚 부산도서관 관리 시스템 API 명세서 (API_DOCS)

본 문서는 **Spring Boot (Main API)** 및 **Flask (AI API)** 백엔드에서 제공되는 주요 RESTful 엔드포인트를 정리한 명세서입니다. 프론트엔드 연동(Flutter) 시 참조할 수 있습니다.

---

## 🟢 1. Spring Boot 백엔드 API (Main Server)
> Base URL: `http://10.0.2.2:8080` (에뮬레이터용) 또는 `http://localhost:8080`

### 👤 1.1 회원 관리 (Member & Auth)
| Method | Endpoint | Description | Request | Response |
|---|---|---|---|---|
| `POST` | `/api/member/login` | JWT 로그인 발급 | `email`, `pw` | `accessToken`, `refreshToken`, `nickname` |
| `POST` | `/api/member/signup` | 신규 회원 가입 | `email`, `pw`, `nickname`, `profileImage`(opt) | 성공 메시지 (200 OK) |

### 📖 1.2 도서 서비스 (Book & Rental)
| Method | Endpoint | Description | Request | Response |
|---|---|---|---|---|
| `GET` | `/api/library/book/list` | 전체 도서 검색 및 페이징 | `page`, `size`, `keyword` | `PageResponseDTO<BookDTO>` |
| `GET` | `/api/library/book/{id}` | 특정 도서 상세 정보(QR) | 경로 파라미터 `id` | `BookDTO` |
| `GET` | `/api/library/rental/my` | 내 도서 대여 이력 조회 | `uid` (또는 JWT 자동 연동) | `List<RentalDTO>` |
| `POST` | `/api/library/rental/request` | 도서 대여/예약 신청 | `bno`, `uid` | 성공 상태 반환 |

### 🏛 1.3 커뮤니티 및 시설 (Notice, Event, Reserve)
| Method | Endpoint | Description | Request | Response |
|---|---|---|---|---|
| `GET` | `/api/library/notice/list` | 공지사항 리스트 조회 | `page`, `size` | `PageResponseDTO<NoticeDTO>` |
| `GET` | `/api/library/event/list` | 이달의 행사 목록 | `page`, `size` | `PageResponseDTO<LibraryEventDTO>` |
| `GET` | `/api/library/apply/status` | 열람실/스터디룸 상태 | `facilityType` | 사용 중인 예약 현황 목록 |

### 💬 1.4 문의 게시판 (Inquiry)
| Method | Endpoint | Description | Request | Response |
|---|---|---|---|---|
| `GET` | `/api/library/inquiry/my` | 나의 1:1 문의 내역 | `uid` | `List<InquiryDTO>` |
| `POST` | `/api/library/inquiry/write` | 신규 문의 작성 | `title`, `content`, `uid` | 성공 메시지 반환 |

---

## 🤖 2. Flask AI 백엔드 API (AI Server)
> Base URL: `http://10.0.2.2:5000` (에뮬레이터용) 또는 `http://localhost:5000`

### 📷 2.1 AI 이미지 분류 (Vision Classification)
> ResNet50 모델을 사용하여 도서관 앨범/사물을 추론합니다. `config.py`의 VISION_MODEL_CONFIGS 설정과 연결되어 모바일앱 화면에서 자동 호출됩니다.

| Method | Endpoint | Description | Model Configuration Target |
|---|---|---|---|
| `POST` | `/predict/animal` | 동물/동물도감 사진 5종 분류 | `animal` 카테고리 로드 |
| `POST` | `/predict/appliance`| 구형/신형 폐가전 및 기기 13종 분류 | `appliance` 카테고리 로드 |
| `POST` | `/predict/tool` | 작업 공구 관련 사물 10종 분류 | `tool` 카테고리 로드 |

- **Request Form-Data:**
  - `image`: 사용자가 촬영한 이미지 파일 객체
- **Response JSON Sample:**
  ```json
  {
    "filename": "sample1.jpg",
    "predicted_class": "고양이",
    "confidence": "98.5%"
  }
  ```

### 📈 2.2 부가 기능 (YOLO 및 딥러닝 시계열)
| Method | Endpoint | Description | Response |
|---|---|---|---|
| `POST` | `/predict/yolo` | YOLOv8을 거쳐 객체 탐지 렌더링된 원본 이미지 반환 | 탐지 완료 비디오/이미지 URL |
| `POST` | `/api/predict2/{model_type}` | 삼성 주가 등 LSTM/RNN 기반 시계열 딥러닝 모델 사용 | JSON `prediction` 예측 금액 반환 |
