package com.busanit501.api5012.service.library;

import com.busanit501.api5012.domain.library.Book;
import com.busanit501.api5012.domain.library.BookStatus;
import com.busanit501.api5012.domain.library.Member;
import com.busanit501.api5012.domain.library.MemberRole;
import com.busanit501.api5012.domain.library.Rental;
import com.busanit501.api5012.domain.library.RentalStatus;
import com.busanit501.api5012.dto.library.BookDTO;
import com.busanit501.api5012.repository.library.BookRepository;
import com.busanit501.api5012.repository.library.MemberRepository;
import com.busanit501.api5012.repository.library.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BookServiceIntegrationTest - BookService.updateBook() 상태 변경 시나리오 검증
 *
 * [검증 목적]
 * 관리자 도서 수정 화면에서 상태를 RENTED → AVAILABLE 로 변경 저장할 때
 * 1. Book.status 가 실제로 AVAILABLE 로 업데이트되는지
 * 2. 활성 대여 기록(Rental)이 자동으로 RETURNED 로 동기화되는지
 * 3. returnDate 가 오늘 날짜로 설정되는지
 * 확인하여 "도서 관리 화면에서 대출가능으로 변경해도 목록에는 여전히 대여중" 이라는
 * 사용자 보고 버그의 백엔드 측 동작을 검증합니다.
 *
 * [테스트 격리]
 * @Transactional 로 각 테스트 종료 후 DB 상태 자동 롤백.
 */
@SpringBootTest
@Transactional
class BookServiceIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private RentalService rentalService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Long memberId;
    private Long bookId;

    @BeforeEach
    void setUp() {
        Member member = Member.builder()
                .mid("bookupdate_" + System.currentTimeMillis())
                .mpw("password123!")
                .mname("상태변경테스트회원")
                .email("bookupdate_" + System.currentTimeMillis() + "@test.com")
                .role(MemberRole.USER)
                .build();
        memberId = memberRepository.save(member).getId();

        Book book = Book.builder()
                .bookTitle("상태 변경 시나리오 검증 도서")
                .author("테스트 저자")
                .publisher("테스트 출판사")
                .isbn("UB" + System.currentTimeMillis())
                .description("기존 설명")
                .status(BookStatus.AVAILABLE)
                .build();
        bookId = bookRepository.save(book).getId();
    }

    // ─────────────────────────────────────────────────────────────────
    // 핵심 검증 : 관리자 편집 화면 시나리오 재현
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateBook() - RENTED → AVAILABLE 상태 변경 시 Book.status 와 Rental.status 동시 업데이트")
    void updateBook_rentedToAvailable_shouldCascadeRentalReturn() {
        // given - 먼저 도서를 대여 처리하여 RENTED 상태로 만듦
        Long rentalId = rentalService.rentBook(memberId, bookId);

        // 사전 검증 : 대여 후 상태
        assertThat(bookRepository.findById(bookId).orElseThrow().getStatus())
                .isEqualTo(BookStatus.RENTED);
        assertThat(rentalRepository.findById(rentalId).orElseThrow().getStatus())
                .isEqualTo(RentalStatus.RENTING);

        // when - 관리자 편집 화면에서 보내는 DTO 구성 (status 필드 포함)
        BookDTO dto = BookDTO.builder()
                .id(bookId)
                .bookTitle("수정된 제목")
                .author("수정된 저자")
                .publisher("수정된 출판사")
                .description("수정된 설명")
                .status(BookStatus.AVAILABLE)  // ← 관리자 드롭다운 선택값
                .build();
        bookService.updateBook(bookId, dto);

        // then - Book 테이블 검증 (핵심)
        Book updatedBook = bookRepository.findById(bookId).orElseThrow();
        assertThat(updatedBook.getStatus())
                .as("updateBook() 후 Book.status 는 AVAILABLE 이어야 합니다")
                .isEqualTo(BookStatus.AVAILABLE);
        assertThat(updatedBook.getBookTitle()).isEqualTo("수정된 제목");

        // then - Rental 테이블 검증 (자동 반납 동기화)
        Rental rental = rentalRepository.findById(rentalId).orElseThrow();
        assertThat(rental.getStatus())
                .as("updateBook() 로 AVAILABLE 로 바꾸면 Rental.status 도 RETURNED 로 동기화되어야 합니다")
                .isEqualTo(RentalStatus.RETURNED);
        assertThat(rental.getReturnDate())
                .as("returnDate 가 오늘 날짜로 자동 설정되어야 합니다")
                .isNotNull();
    }

    @Test
    @DisplayName("updateBook() - AVAILABLE → LOST 상태 변경 시 Book.status 만 업데이트")
    void updateBook_availableToLost_shouldChangeStatusOnly() {
        // given - AVAILABLE 상태 (setUp 에서 생성됨)
        assertThat(bookRepository.findById(bookId).orElseThrow().getStatus())
                .isEqualTo(BookStatus.AVAILABLE);

        // when - LOST 로 변경
        BookDTO dto = BookDTO.builder()
                .id(bookId)
                .bookTitle("상태 변경 시나리오 검증 도서")
                .author("테스트 저자")
                .publisher("테스트 출판사")
                .description("기존 설명")
                .status(BookStatus.LOST)
                .build();
        bookService.updateBook(bookId, dto);

        // then
        assertThat(bookRepository.findById(bookId).orElseThrow().getStatus())
                .isEqualTo(BookStatus.LOST);
    }

    @Test
    @DisplayName("updateBook() - status 필드가 null 이면 기존 상태 유지")
    void updateBook_statusNull_shouldKeepExistingStatus() {
        // given
        assertThat(bookRepository.findById(bookId).orElseThrow().getStatus())
                .isEqualTo(BookStatus.AVAILABLE);

        // when - status 없이 정보만 수정
        BookDTO dto = BookDTO.builder()
                .id(bookId)
                .bookTitle("제목만 수정")
                .author("테스트 저자")
                .publisher("테스트 출판사")
                .description("기존 설명")
                // status 없음
                .build();
        bookService.updateBook(bookId, dto);

        // then - 상태는 그대로, 제목만 변경
        Book updated = bookRepository.findById(bookId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BookStatus.AVAILABLE);
        assertThat(updated.getBookTitle()).isEqualTo("제목만 수정");
    }
}
