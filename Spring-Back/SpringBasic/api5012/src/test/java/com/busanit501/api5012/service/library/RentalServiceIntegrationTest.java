package com.busanit501.api5012.service.library;

import com.busanit501.api5012.domain.library.Book;
import com.busanit501.api5012.domain.library.BookStatus;
import com.busanit501.api5012.domain.library.Member;
import com.busanit501.api5012.domain.library.MemberRole;
import com.busanit501.api5012.domain.library.Rental;
import com.busanit501.api5012.domain.library.RentalStatus;
import com.busanit501.api5012.repository.library.BookRepository;
import com.busanit501.api5012.repository.library.MemberRepository;
import com.busanit501.api5012.repository.library.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RentalServiceIntegrationTest - 대여/반납 연관 관계 통합 테스트
 *
 * [검증 목표]
 * 1. rentBook() 호출 시 Rental(RENTING) + Book(RENTED) 두 테이블이 동시에 업데이트되는지 확인
 * 2. returnBook() 호출 시 Rental(RETURNED, returnDate 설정) + Book(AVAILABLE) 두 테이블이
 *    동시에 업데이트되는지 확인
 * 3. 이미 반납된 대여 기록을 다시 반납 시도하면 예외가 발생하는지 확인
 *
 * [테스트 격리]
 * @Transactional 을 사용하여 각 테스트 종료 후 DB 상태를 자동 롤백합니다.
 * 테스트 간 데이터가 오염되지 않습니다.
 */
@SpringBootTest
@Transactional
class RentalServiceIntegrationTest {

    @Autowired
    private RentalService rentalService;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Long memberId;
    private Long bookId;

    /**
     * 각 테스트 실행 전 회원 1명, 도서 1권을 DB에 저장합니다.
     * @Transactional 로 인해 테스트 종료 시 자동 롤백됩니다.
     */
    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        Member member = Member.builder()
                .mid("testuser_" + System.currentTimeMillis())
                .mpw("password123!")
                .mname("테스트회원")
                .email("test_" + System.currentTimeMillis() + "@test.com")
                .role(MemberRole.USER)
                .build();
        memberId = memberRepository.save(member).getId();

        // 테스트용 도서 생성 (AVAILABLE 상태)
        Book book = Book.builder()
                .bookTitle("테스트 도서 - 연관관계 검증")
                .author("테스트 저자")
                .publisher("테스트 출판사")
                .isbn("RT" + System.currentTimeMillis())
                .status(BookStatus.AVAILABLE)
                .build();
        bookId = bookRepository.save(book).getId();
    }

    // ─────────────────────────────────────────────────────────────────
    // 대여(rentBook) 연관 관계 검증
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("rentBook() - Rental.status=RENTING, Book.status=RENTED 동시 업데이트 검증")
    void rentBook_shouldUpdateBothRentalAndBookStatus() {
        // when
        Long rentalId = rentalService.rentBook(memberId, bookId);

        // then - Rental 테이블 검증
        Rental rental = rentalRepository.findById(rentalId).orElseThrow();
        assertThat(rental.getStatus()).isEqualTo(RentalStatus.RENTING);
        assertThat(rental.getRentalDate()).isEqualTo(LocalDate.now());
        assertThat(rental.getDueDate()).isEqualTo(LocalDate.now().plusDays(14));
        assertThat(rental.getReturnDate()).isNull();
        assertThat(rental.getBook().getId()).isEqualTo(bookId);
        assertThat(rental.getMember().getId()).isEqualTo(memberId);

        // then - Book 테이블 검증
        Book book = bookRepository.findById(bookId).orElseThrow();
        assertThat(book.getStatus()).isEqualTo(BookStatus.RENTED);
    }

    // ─────────────────────────────────────────────────────────────────
    // 반납(returnBook) 연관 관계 검증 — 핵심 검증
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("returnBook() - Rental.status=RETURNED + returnDate 설정, Book.status=AVAILABLE 동시 업데이트 검증")
    void returnBook_shouldUpdateBothRentalAndBookStatus() {
        // given - 먼저 대여 처리
        Long rentalId = rentalService.rentBook(memberId, bookId);

        // 대여 후 도서 상태 사전 검증
        assertThat(bookRepository.findById(bookId).orElseThrow().getStatus())
                .isEqualTo(BookStatus.RENTED);

        // when - 반납 처리
        rentalService.returnBook(rentalId);

        // then - Rental 테이블 검증
        Rental rental = rentalRepository.findById(rentalId).orElseThrow();
        assertThat(rental.getStatus())
                .as("반납 후 Rental.status 는 RETURNED 이어야 합니다")
                .isEqualTo(RentalStatus.RETURNED);
        assertThat(rental.getReturnDate())
                .as("반납일(returnDate)이 오늘 날짜로 설정되어야 합니다")
                .isEqualTo(LocalDate.now());

        // then - Book 테이블 검증 (핵심: 도서 상태도 함께 변경됐는지 확인)
        Book book = bookRepository.findById(bookId).orElseThrow();
        assertThat(book.getStatus())
                .as("반납 후 Book.status 는 AVAILABLE 이어야 합니다")
                .isEqualTo(BookStatus.AVAILABLE);
    }

    @Test
    @DisplayName("returnBook() - 이미 반납된 기록 재반납 시 IllegalStateException 발생 검증")
    void returnBook_alreadyReturned_shouldThrowException() {
        // given
        Long rentalId = rentalService.rentBook(memberId, bookId);
        rentalService.returnBook(rentalId); // 첫 번째 반납

        // when & then - 두 번째 반납 시 예외
        assertThatThrownBy(() -> rentalService.returnBook(rentalId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 반납 처리된 기록");
    }

    // ─────────────────────────────────────────────────────────────────
    // 대여 불가 도서 대여 시도 검증
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("rentBook() - 이미 RENTED 상태 도서 대여 시도 시 IllegalStateException 발생 검증")
    void rentBook_alreadyRented_shouldThrowException() {
        // given - 첫 번째 대여
        rentalService.rentBook(memberId, bookId);

        // 두 번째 회원 생성
        Member anotherMember = Member.builder()
                .mid("another_" + System.currentTimeMillis())
                .mpw("password!")
                .mname("다른회원")
                .email("another_" + System.currentTimeMillis() + "@test.com")
                .role(MemberRole.USER)
                .build();
        Long anotherMemberId = memberRepository.save(anotherMember).getId();

        // when & then - 같은 도서 중복 대여 시 예외
        assertThatThrownBy(() -> rentalService.rentBook(anotherMemberId, bookId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("대여 불가능한 도서");
    }

    // ─────────────────────────────────────────────────────────────────
    // 대여-반납 전체 사이클 검증
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("대여 → 반납 → 재대여 전체 사이클 검증")
    void fullCycle_rent_return_rentAgain() {
        // 1회 대여
        Long rentalId1 = rentalService.rentBook(memberId, bookId);
        assertThat(bookRepository.findById(bookId).orElseThrow().getStatus())
                .isEqualTo(BookStatus.RENTED);

        // 반납
        rentalService.returnBook(rentalId1);
        assertThat(bookRepository.findById(bookId).orElseThrow().getStatus())
                .isEqualTo(BookStatus.AVAILABLE);

        // 2회 대여 (반납 후 재대여 가능해야 함)
        Long rentalId2 = rentalService.rentBook(memberId, bookId);
        assertThat(rentalId2).isNotNull();
        assertThat(bookRepository.findById(bookId).orElseThrow().getStatus())
                .isEqualTo(BookStatus.RENTED);

        // 2회 반납
        rentalService.returnBook(rentalId2);
        assertThat(bookRepository.findById(bookId).orElseThrow().getStatus())
                .isEqualTo(BookStatus.AVAILABLE);
    }
}
