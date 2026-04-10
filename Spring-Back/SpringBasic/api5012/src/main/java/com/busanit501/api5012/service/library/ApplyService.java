package com.busanit501.api5012.service.library;

import com.busanit501.api5012.dto.library.ApplyDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * ApplyService - 시설 예약 서비스 인터페이스
 *
 * 부산도서관 관리 시스템의 시설 예약 신청 비즈니스 로직을 정의합니다.
 * 세미나실, 스터디룸, 강당 등 도서관 시설의 예약을 관리합니다.
 *
 * [예약 처리 흐름]
 * 1. 회원이 예약 신청 → 상태: PENDING (대기)
 * 2. 관리자가 신청 확인 후 approveApply() → 상태: APPROVED (승인)
 *    또는 rejectApply() → 상태: REJECTED (반려)
 *
 * [중복 예약 방지]
 * 같은 날짜에 같은 시설을 PENDING 또는 APPROVED 상태로 예약한 건이 이미 있으면
 * 새 예약 신청을 거부합니다.
 */
public interface ApplyService {

    /**
     * createApply - 시설 예약 신청 등록
     *
     * 1. 회원 존재 여부 확인
     * 2. 같은 날짜·같은 시설 중복 예약 확인
     * 3. Apply 엔티티 생성 (상태: PENDING)
     *
     * @param dto      예약 신청 정보 (시설 유형, 날짜, 신청자명, 연락처, 인원)
     * @param memberId 신청 회원 ID
     * @return 생성된 예약 신청 ID
     * @throws IllegalStateException 중복 예약 시
     */
    Long createApply(ApplyDTO dto, Long memberId);

    /**
     * getMyApplies - 내 예약 신청 목록 조회
     *
     * 마이페이지에서 내가 신청한 시설 예약 목록을 조회합니다.
     *
     * @param memberId 조회할 회원 ID
     * @param pageable 페이지 정보
     * @return 페이지네이션이 적용된 예약 신청 목록
     */
    Page<ApplyDTO> getMyApplies(Long memberId, Pageable pageable);

    /**
     * approveApply - 예약 신청 승인 (관리자 전용)
     *
     * 대기 중(PENDING) 상태의 예약을 승인(APPROVED) 처리합니다.
     *
     * @param applyId 승인할 예약 신청 ID
     * @throws RuntimeException 예약 기록 없음, 이미 처리된 경우
     */
    void approveApply(Long applyId);

    /**
     * rejectApply - 예약 신청 반려 (관리자 전용)
     *
     * 대기 중(PENDING) 상태의 예약을 반려(REJECTED) 처리합니다.
     *
     * @param applyId 반려할 예약 신청 ID
     * @throws RuntimeException 예약 기록 없음, 이미 처리된 경우
     */
    void rejectApply(Long applyId);

    /**
     * getAllApplies - 전체 예약 신청 목록 조회 (관리자 전용)
     * 모든 회원의 시설 예약 신청 목록을 페이지 단위로 반환합니다.
     *
     * @param pageable 페이지 정보
     * @return 페이지네이션이 적용된 전체 예약 신청 목록
     */
    Page<ApplyDTO> getAllApplies(Pageable pageable);

    /**
     * createApplyAsAdmin - 관리자 직접 예약 등록
     * 관리자가 특정 회원을 대신해 예약을 등록합니다.
     *
     * @param dto      예약 신청 정보
     * @param memberId 신청 회원 ID
     * @return 생성된 예약 기본키
     */
    Long createApplyAsAdmin(ApplyDTO dto, Long memberId);

    /**
     * updateApply - 예약 정보 수정 (관리자 전용)
     * 신청자명/시설/연락처/인원/예약일 및 상태를 수정합니다.
     *
     * @param id  수정할 예약 기본키
     * @param dto 수정할 정보
     */
    void updateApply(Long id, ApplyDTO dto);

    /**
     * deleteApply - 예약 삭제 (관리자 전용)
     *
     * @param id 삭제할 예약 기본키
     */
    void deleteApply(Long id);
}
