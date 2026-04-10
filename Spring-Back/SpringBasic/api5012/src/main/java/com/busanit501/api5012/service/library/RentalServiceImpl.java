package com.busanit501.api5012.service.library;

import com.busanit501.api5012.domain.library.Book;
import com.busanit501.api5012.domain.library.BookStatus;
import com.busanit501.api5012.domain.library.Member;
import com.busanit501.api5012.domain.library.Rental;
import com.busanit501.api5012.domain.library.RentalStatus;
import com.busanit501.api5012.dto.library.RentalDTO;
import com.busanit501.api5012.repository.library.BookRepository;
import com.busanit501.api5012.repository.library.MemberRepository;
import com.busanit501.api5012.repository.library.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * RentalServiceImpl - 도서 대여 서비스 구현체
 *
 * RentalService 인터페이스의 구현 클래스입니다.
 * 도서 대여, 반납, 연장 처리와 대여 목록 조회 기능을 구현합니다.
 *
 * [대여 정책]
 * - 대여 기간  : 14일 (기본)
 * - 연장       : 7일 추가
 * - 최대 대여 권수 : 3권
 * - 동일 도서 중복 대여 불가
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalServiceImpl implements RentalService {

    /** RentalRepository - 대여 기록 엔티티에 대한 DB 접근 */
    private final RentalRepository rentalRepository;

    /** MemberRepository - 회원 엔티티에 대한 DB 접근 */
    private final MemberRepository memberRepository;

    /** BookRepository - 도서 엔티티에 대한 DB 접근 */
    private final BookRepository bookRepository;

    /** 기본 대여 기간 (일수) */
    private static final int DEFAULT_RENTAL_DAYS = 14;

    /** 연장 일수 */
    private static final int EXTENSION_DAYS = 7;

    /** 최대 동시 대여 권수 */
    private static final int MAX_RENTAL_COUNT = 3;

    /**
     * rentBook - 도서 대여 처리
     *
     * [핵심 로직]
     * 1. 회원 조회 → 없으면 예외
     * 2. 도서 조회 → 없으면 예외
     * 3. 도서 상태 확인 → AVAILABLE 이 아니면 예외
     * 4. 현재 대여 건수 확인 → MAX(3권) 초과 시 예외
     * 5. 동일 도서 이미 대여 중 확인 → 중복이면 예외
     * 6. Rental 엔티티 생성 + Book 상태 RENTED 로 변경
     *
     * [LocalDate 사용]
     * LocalDate.now()          : 오늘 날짜
     * LocalDate.now().plusDays(14) : 오늘 + 14일 (반납 기한)
     */
    @Override
    @Transactional
    public Long rentBook(Long memberId, Long bookId) {
        log.info("도서 대여 처리 시작 - memberId: {}, bookId: {}", memberId, bookId);

        // 1단계: 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. id: " + memberId));

        // 2단계: 도서 조회
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("도서를 찾을 수 없습니다. id: " + bookId));

        // 3단계: 도서 대여 가능 상태 확인
        if (!book.isAvailable()) {
            throw new IllegalStateException(
                    "대여 불가능한 도서입니다. 현재 상태: " + book.getStatus());
        }

        // 4단계: 현재 대여 건수 확인 (RENTING 상태인 것만 집계)
        long currentRentalCount = rentalRepository.countByMemberIdAndStatus(
                memberId, RentalStatus.RENTING);
        if (currentRentalCount >= MAX_RENTAL_COUNT) {
            throw new IllegalStateException(
                    "최대 대여 가능 권수(" + MAX_RENTAL_COUNT + "권)를 초과하였습니다. "
                    + "현재 대여 중: " + currentRentalCount + "권");
        }

        // 5단계: 동일 도서 중복 대여 확인
        boolean alreadyRented = rentalRepository.existsByMemberIdAndBookIdAndStatus(
                memberId, bookId, RentalStatus.RENTING);
        if (alreadyRented) {
            throw new IllegalStateException("이미 대여 중인 도서입니다. bookId: " + bookId);
        }

        // 6단계: Rental 엔티티 생성
        LocalDate today = LocalDate.now();
        Rental rental = Rental.builder()
                .member(member)
                .book(book)
                .rentalDate(today)
                .dueDate(today.plusDays(DEFAULT_RENTAL_DAYS)) // 반납 기한: 14일 후
                .status(RentalStatus.RENTING)
                .build();

        Long rentalId = rentalRepository.save(rental).getId();

        // 7단계: 도서 상태를 RENTED 로 변경 (더티체킹으로 자동 UPDATE)
        book.changeStatus(BookStatus.RENTED);

        log.info("도서 대여 완료 - rentalId: {}, memberId: {}, bookId: {}, 반납기한: {}",
                rentalId, memberId, bookId, today.plusDays(DEFAULT_RENTAL_DAYS));

        return rentalId;
    }

    /**
     * returnBook - 도서 반납 처리
     *
     * Rental 엔티티의 processReturn() 도메인 메서드를 호출합니다.
     * 반납 후 도서 상태를 AVAILABLE 로 변경합니다.
     */
    @Override
    @Transactional
    public void returnBook(Long rentalId) {
        log.info("도서 반납 처리 시작 - rentalId: {}", rentalId);

        // 대여 기록 조회
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new RuntimeException("대여 기록을 찾을 수 없습니다. id: " + rentalId));

        // 이미 반납된 기록인지 확인
        if (rental.getStatus() == RentalStatus.RETURNED) {
            throw new IllegalStateException("이미 반납 처리된 기록입니다. rentalId: " + rentalId);
        }

        // 도메인 메서드를 통한 반납 처리 (returnDate 기록 + 상태 RETURNED 변경)
        rental.processReturn(LocalDate.now());

        // 해당 도서 상태를 AVAILABLE 로 복원 (더티체킹으로 자동 UPDATE)
        rental.getBook().changeStatus(BookStatus.AVAILABLE);

        log.info("도서 반납 완료 - rentalId: {}, 반납일: {}", rentalId, LocalDate.now());
    }

    /**
     * extendRental - 반납 기한 연장 (7일)
     *
     * Rental 엔티티의 extendDueDate() 도메인 메서드를 호출합니다.
     */
    @Override
    @Transactional
    public void extendRental(Long rentalId) {
        log.info("대여 연장 처리 시작 - rentalId: {}", rentalId);

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new RuntimeException("대여 기록을 찾을 수 없습니다. id: " + rentalId));

        // 반납된 기록은 연장 불가
        if (rental.getStatus() == RentalStatus.RETURNED) {
            throw new IllegalStateException("이미 반납된 도서는 연장할 수 없습니다. rentalId: " + rentalId);
        }

        // 현재 반납 기한에서 7일 추가 후 상태 EXTENDED 로 변경
        LocalDate newDueDate = rental.getDueDate().plusDays(EXTENSION_DAYS);
        rental.extendDueDate(newDueDate);

        log.info("대여 연장 완료 - rentalId: {}, 새 반납기한: {}", rentalId, newDueDate);
    }

    /**
     * getMyRentals - 회원의 전체 대여 목록 조회 (페이지네이션)
     *
     * JOIN FETCH 쿼리로 member 와 book 정보를 한 번에 로딩합니다. (N+1 문제 방지)
     * Page<Rental> 을 Page<RentalDTO> 로 변환하여 반환합니다.
     *
     * [N+1 문제 설명]
     * LAZY 로딩 엔티티를 반복문에서 접근하면, 각 엔티티마다 추가 SELECT 가 발생합니다.
     * N 개의 대여 기록이 있으면 N개의 추가 쿼리가 발생합니다. → "N+1 문제"
     * JOIN FETCH 를 사용하면 하나의 쿼리로 연관 엔티티를 함께 로딩하여 방지합니다.
     */
    @Override
    public Page<RentalDTO> getMyRentals(Long memberId, Pageable pageable) {
        log.info("대여 목록 조회 - memberId: {}, page: {}", memberId, pageable.getPageNumber());

        // JOIN FETCH 쿼리로 member + book 한 번에 로딩
        Page<Rental> rentalPage = rentalRepository.findByMemberId(memberId, pageable);

        // Page<Rental> → Page<RentalDTO> 변환
        return rentalPage.map(RentalDTO::fromEntity);
    }

    /**
     * getMyRentalsByStatus - 상태별 대여 목록 조회
     *
     * List 로 반환하므로 페이지네이션 없이 전체 결과를 반환합니다.
     * Stream API 로 Rental 엔티티를 RentalDTO 로 변환합니다.
     *
     * [Stream API collect() 설명]
     * .stream()                          : List 를 Stream 으로 변환
     * .map(RentalDTO::fromEntity)        : 각 Rental 을 RentalDTO 로 변환 (메서드 참조)
     * .collect(Collectors.toList())      : Stream 결과를 List 로 수집
     */
    @Override
    public List<RentalDTO> getMyRentalsByStatus(Long memberId, RentalStatus status) {
        log.info("상태별 대여 목록 조회 - memberId: {}, status: {}", memberId, status);

        List<Rental> rentals = rentalRepository.findByMemberIdAndStatus(memberId, status);

        return rentals.stream()
                .map(RentalDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * getActiveRentalByBookId - 도서의 현재 활성 대여 기록 조회 (관리자용)
     */
    @Override
    public Optional<RentalDTO> getActiveRentalByBookId(Long bookId) {
        log.info("도서 활성 대여 조회 - bookId: {}", bookId);
        return rentalRepository.findActiveRentalByBookId(bookId)
                .map(RentalDTO::fromEntity);
    }

    /**
     * getAllRentals - 전체 대여 목록 조회 (관리자용)
     */
    @Override
    public Page<RentalDTO> getAllRentals(Pageable pageable) {
        log.info("전체 대여 목록 조회 (관리자) - page: {}", pageable.getPageNumber());
        return rentalRepository.findAllWithDetails(pageable).map(RentalDTO::fromEntity);
    }
}
