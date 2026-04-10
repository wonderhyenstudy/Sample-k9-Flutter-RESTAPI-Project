package com.busanit501.api5012.repository.library;

import com.busanit501.api5012.domain.library.Rental;
import com.busanit501.api5012.domain.library.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * RentalRepository - 도서 대여 기록 레포지토리
 *
 * Rental 엔티티에 대한 DB 접근 인터페이스입니다.
 * 회원별 대여 이력 조회, 도서별 대여 상태 확인, 연체 도서 조회 등의 기능을 제공합니다.
 *
 * [JOIN FETCH 설명]
 * @Query 에서 JOIN FETCH 를 사용하면 연관 엔티티(member, book)를
 * 한 번의 쿼리로 함께 로딩합니다. (N+1 문제 해결)
 * LAZY 로딩이지만 필요한 데이터를 미리 가져와 추가 쿼리를 방지합니다.
 */
@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {

    /**
     * findByMemberId - 회원 ID 로 대여 기록 전체 조회
     * 마이페이지의 대여 이력 화면에서 사용합니다.
     * JOIN FETCH 로 member 와 book 정보를 함께 로딩합니다.
     *
     * @param memberId 조회할 회원의 ID (기본키)
     * @param pageable 페이징 정보
     * @return 해당 회원의 모든 대여 기록 (페이징)
     */
    @Query("SELECT r FROM Rental r " +
           "JOIN FETCH r.member m " +
           "JOIN FETCH r.book b " +
           "WHERE m.id = :memberId " +
           "ORDER BY r.rentalDate DESC")
    Page<Rental> findByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    /**
     * findByMemberIdAndStatus - 회원 ID + 상태로 대여 기록 조회
     * 현재 대여 중인 도서 목록 (status = RENTING) 또는
     * 반납 완료 목록 (status = RETURNED) 조회에 사용합니다.
     *
     * @param memberId 조회할 회원의 ID
     * @param status   조회할 대여 상태
     * @return 조건에 맞는 대여 기록 목록
     */
    List<Rental> findByMemberIdAndStatus(Long memberId, RentalStatus status);

    /**
     * findByBookIdAndStatus - 도서 ID + 상태로 대여 기록 조회
     * 특정 도서의 현재 대여 상태를 확인합니다.
     * 예: 도서가 현재 대여 중인지 확인 (status = RENTING)
     *
     * @param bookId 조회할 도서의 ID
     * @param status 조회할 대여 상태
     * @return 조건에 맞는 대여 기록 목록
     */
    List<Rental> findByBookIdAndStatus(Long bookId, RentalStatus status);

    /**
     * findActiveRentalByBookId - 도서의 현재 활성 대여 기록 조회
     * 반납 처리 시 해당 도서의 현재 대여 기록을 찾습니다.
     * RENTING 또는 EXTENDED 상태의 기록을 조회합니다.
     *
     * @param bookId 조회할 도서의 ID
     * @return 현재 활성 대여 기록 (없으면 Optional.empty())
     */
    @Query("SELECT r FROM Rental r " +
           "WHERE r.book.id = :bookId " +
           "AND r.status IN ('RENTING', 'EXTENDED') " +
           "ORDER BY r.id DESC")
    List<Rental> findActiveRentalsByBookId(@Param("bookId") Long bookId);

    /**
     * findActiveRentalByBookId - 도서의 현재 활성 대여 기록 조회 (최신 1건)
     *
     * 데이터 정합성이 깨진 경우(같은 도서에 활성 대여 2건 이상 존재) 대비용으로
     * List 반환 메서드를 래핑하여 가장 최신(id DESC) 기록만 반환합니다.
     *
     * @param bookId 조회할 도서의 ID
     * @return 가장 최근 활성 대여 기록 (없으면 Optional.empty())
     */
    default Optional<Rental> findActiveRentalByBookId(Long bookId) {
        List<Rental> rentals = findActiveRentalsByBookId(bookId);
        return rentals.isEmpty() ? Optional.empty() : Optional.of(rentals.get(0));
    }

    /**
     * findOverdueRentals - 연체 대여 기록 전체 조회
     * 반납 기한(dueDate)이 오늘보다 이전이고 아직 반납되지 않은 기록을 조회합니다.
     * 스케줄러에서 매일 자동 연체 처리할 때 사용합니다.
     *
     * @param today 오늘 날짜 (LocalDate.now())
     * @return 연체된 대여 기록 목록
     */
    @Query("SELECT r FROM Rental r " +
           "WHERE r.dueDate < :today " +
           "AND r.status IN ('RENTING', 'EXTENDED')")
    List<Rental> findOverdueRentals(@Param("today") LocalDate today);

    /**
     * findByMemberIdOrderByRentalDateDesc - 회원의 최신 대여 기록 조회
     * 최근 대여 순으로 정렬된 대여 기록을 조회합니다.
     *
     * @param memberId 조회할 회원 ID
     * @return 최신 대여순 대여 기록 목록
     */
    List<Rental> findByMemberIdOrderByRentalDateDesc(Long memberId);

    /**
     * countByMemberIdAndStatus - 회원의 상태별 대여 건수 집계
     * 현재 대여 중인 도서 수를 확인하여 대여 한도(예: 최대 3권) 초과 여부를 검증합니다.
     *
     * @param memberId 조회할 회원 ID
     * @param status   집계할 대여 상태 (주로 RENTING)
     * @return 해당 상태의 대여 건수
     */
    long countByMemberIdAndStatus(Long memberId, RentalStatus status);

    /**
     * existsByMemberIdAndBookIdAndStatus - 특정 도서의 중복 대여 확인
     * 같은 회원이 동일한 도서를 이미 대여 중인지 확인합니다.
     *
     * @param memberId 회원 ID
     * @param bookId   도서 ID
     * @param status   대여 상태 (RENTING)
     * @return 이미 대여 중이면 true
     */
    boolean existsByMemberIdAndBookIdAndStatus(Long memberId, Long bookId, RentalStatus status);

    /**
     * findAllWithDetails - 전체 대여 목록 조회 (관리자용, JOIN FETCH)
     * 관리자 대여 관리 화면에서 회원 정보와 도서 정보를 함께 표시합니다.
     * JOIN FETCH 로 N+1 문제를 방지합니다.
     *
     * @param pageable 페이징 정보
     * @return 대여 기록 목록 (회원·도서 정보 포함)
     */
    @Query("SELECT r FROM Rental r " +
           "JOIN FETCH r.member " +
           "JOIN FETCH r.book " +
           "ORDER BY r.rentalDate DESC")
    Page<Rental> findAllWithDetails(Pageable pageable);
}
