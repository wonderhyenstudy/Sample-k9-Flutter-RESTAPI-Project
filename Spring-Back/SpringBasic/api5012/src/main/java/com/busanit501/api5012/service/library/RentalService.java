package com.busanit501.api5012.service.library;

import com.busanit501.api5012.domain.library.RentalStatus;
import com.busanit501.api5012.dto.library.RentalDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * RentalService - 도서 대여 서비스 인터페이스
 *
 * 부산도서관 관리 시스템의 도서 대여/반납/연장 비즈니스 로직을 정의합니다.
 *
 * [대여 처리 흐름]
 * 1. rentBook()   : 회원이 도서 대여 신청 → 도서 상태 RENTED 로 변경
 * 2. returnBook() : 회원이 반납 처리     → 도서 상태 AVAILABLE 로 변경
 * 3. extendRental(): 반납 기한 7일 연장  → 대여 기록 상태 EXTENDED 로 변경
 *
 * [대여 제한 정책]
 * - 한 회원이 동시에 대여 가능한 도서 수: 최대 3권
 * - 대여 기간: 14일 (2주)
 * - 연장: 1회, 7일 추가
 */
public interface RentalService {

    /**
     * rentBook - 도서 대여 처리
     *
     * 1. 회원 존재 여부 확인
     * 2. 도서 존재 여부 및 대여 가능(AVAILABLE) 확인
     * 3. 회원의 현재 대여 건수 확인 (최대 3권 제한)
     * 4. 동일 도서 중복 대여 여부 확인
     * 5. Rental 엔티티 생성 (대여일: 오늘, 반납기한: 14일 후)
     * 6. Book 상태 RENTED 로 변경
     *
     * @param memberId 대여 신청 회원 ID
     * @param bookId   대여할 도서 ID
     * @return 생성된 대여 기록 ID
     * @throws RuntimeException 도서 없음, 대여 불가 상태, 대여 한도 초과 시
     */
    Long rentBook(Long memberId, Long bookId);

    /**
     * returnBook - 도서 반납 처리
     *
     * 1. 대여 기록 조회
     * 2. 반납일(오늘) 기록, 상태 RETURNED 로 변경
     * 3. 해당 도서 상태 AVAILABLE 로 변경
     *
     * @param rentalId 반납할 대여 기록 ID
     * @throws RuntimeException 대여 기록 없음, 이미 반납된 경우
     */
    void returnBook(Long rentalId);

    /**
     * extendRental - 반납 기한 연장 (7일)
     *
     * 현재 반납 기한에서 7일을 연장합니다.
     * 상태를 EXTENDED 로 변경합니다.
     *
     * @param rentalId 연장할 대여 기록 ID
     * @throws RuntimeException 대여 기록 없음, 이미 반납된 경우
     */
    void extendRental(Long rentalId);

    /**
     * getMyRentals - 회원의 전체 대여 목록 조회
     *
     * 마이페이지에서 내 대여 이력 전체를 페이지 단위로 조회합니다.
     * 최신 대여순으로 정렬됩니다.
     *
     * @param memberId 조회할 회원 ID
     * @param pageable 페이지 정보
     * @return 페이지네이션이 적용된 대여 목록
     */
    Page<RentalDTO> getMyRentals(Long memberId, Pageable pageable);

    /**
     * getMyRentalsByStatus - 상태별 대여 목록 조회
     *
     * 현재 대여 중(RENTING), 반납 완료(RETURNED), 연체(OVERDUE) 등
     * 특정 상태의 대여 기록만 조회합니다.
     *
     * @param memberId 조회할 회원 ID
     * @param status   조회할 대여 상태
     * @return 해당 상태의 대여 목록
     */
    List<RentalDTO> getMyRentalsByStatus(Long memberId, RentalStatus status);

    /**
     * getActiveRentalByBookId - 도서의 현재 활성 대여 기록 조회 (관리자용)
     * RENTING 또는 EXTENDED 상태의 대여 기록을 반환합니다.
     *
     * @param bookId 조회할 도서 ID
     * @return 활성 대여 기록 (없으면 empty)
     */
    Optional<RentalDTO> getActiveRentalByBookId(Long bookId);

    /**
     * getAllRentals - 전체 대여 목록 조회 (관리자용)
     *
     * @param pageable 페이지 정보
     * @return 전체 대여 목록 (회원·도서 정보 포함)
     */
    Page<RentalDTO> getAllRentals(Pageable pageable);
}
