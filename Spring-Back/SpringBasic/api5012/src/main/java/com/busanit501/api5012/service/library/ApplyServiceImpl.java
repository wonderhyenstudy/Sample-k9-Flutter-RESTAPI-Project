package com.busanit501.api5012.service.library;

import com.busanit501.api5012.domain.library.Apply;
import com.busanit501.api5012.domain.library.Member;
import com.busanit501.api5012.dto.library.ApplyDTO;
import com.busanit501.api5012.repository.library.ApplyRepository;
import com.busanit501.api5012.repository.library.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ApplyServiceImpl - 시설 예약 서비스 구현체
 *
 * ApplyService 인터페이스의 구현 클래스입니다.
 * 도서관 시설(세미나실, 스터디룸, 강당) 예약 신청, 승인, 반려 기능을 구현합니다.
 *
 * [예약 처리 흐름]
 * 1. 회원이 createApply() 로 예약 신청 → PENDING 상태
 * 2. 관리자가 approveApply() 로 승인   → APPROVED 상태
 *    또는 rejectApply() 로 반려        → REJECTED 상태
 *
 * [중복 예약 정책]
 * 같은 회원이 같은 날짜에 이미 PENDING 상태의 예약 신청이 있으면 새 신청을 거부합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplyServiceImpl implements ApplyService {

    /** ApplyRepository - 시설 예약 엔티티에 대한 DB 접근 */
    private final ApplyRepository applyRepository;

    /** MemberRepository - 회원 엔티티에 대한 DB 접근 */
    private final MemberRepository memberRepository;

    /**
     * createApply - 시설 예약 신청 등록
     *
     * [처리 순서]
     * 1. 회원 조회
     * 2. 동일 날짜 중복 신청 확인 (PENDING 상태만 검사)
     * 3. Apply 엔티티 생성 (상태: PENDING)
     */
    @Override
    @Transactional
    public Long createApply(ApplyDTO dto, Long memberId) {
        log.info("시설 예약 신청 시작 - memberId: {}, 시설: {}, 날짜: {}",
                memberId, dto.getFacilityType(), dto.getReserveDate());

        // 1단계: 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. id: " + memberId));

        // 2단계: 같은 날짜 중복 신청 확인
        // 같은 회원이 같은 날짜에 이미 PENDING 상태의 예약이 있는지 확인합니다.
        boolean isDuplicate = applyRepository.existsByMemberIdAndReserveDateAndStatus(
                memberId, dto.getReserveDate(), "PENDING");
        if (isDuplicate) {
            throw new IllegalStateException(
                    "해당 날짜에 이미 예약 신청이 있습니다. 날짜: " + dto.getReserveDate());
        }

        // 3단계: Apply 엔티티 생성 (빌더 패턴)
        Apply apply = Apply.builder()
                .member(member)
                .applicantName(dto.getApplicantName())
                .facilityType(dto.getFacilityType())
                .phone(dto.getPhone())
                .participants(dto.getParticipants())
                .reserveDate(dto.getReserveDate())
                .build(); // status 기본값: "PENDING", regDate 기본값: now()

        Long savedId = applyRepository.save(apply).getId();
        log.info("시설 예약 신청 완료 - applyId: {}, memberId: {}", savedId, memberId);

        return savedId;
    }

    /**
     * getMyApplies - 내 시설 예약 신청 목록 조회
     *
     * 마이페이지에서 내가 신청한 예약 목록을 페이지 단위로 조회합니다.
     */
    @Override
    public Page<ApplyDTO> getMyApplies(Long memberId, Pageable pageable) {
        log.info("내 예약 신청 목록 조회 - memberId: {}, page: {}", memberId, pageable.getPageNumber());

        Page<Apply> applyPage = applyRepository.findByMemberId(memberId, pageable);
        // Page<Apply> → Page<ApplyDTO> 변환 (메서드 참조)
        return applyPage.map(ApplyDTO::fromEntity);
    }

    /**
     * approveApply - 예약 신청 승인 (관리자 전용)
     *
     * Apply.approve() 도메인 메서드로 상태를 APPROVED 로 변경합니다.
     * @Transactional + 더티체킹으로 자동 UPDATE 됩니다.
     */
    @Override
    @Transactional
    public void approveApply(Long applyId) {
        log.info("예약 신청 승인 시작 - applyId: {}", applyId);

        Apply apply = applyRepository.findById(applyId)
                .orElseThrow(() -> new RuntimeException(
                        "예약 신청을 찾을 수 없습니다. id: " + applyId));

        // 이미 처리된 신청인지 확인 (PENDING 상태가 아니면 처리 불가)
        if (!apply.isPending()) {
            throw new IllegalStateException(
                    "대기 중인 신청만 승인할 수 있습니다. 현재 상태: " + apply.getStatus());
        }

        // 도메인 메서드로 상태 변경 → APPROVED
        apply.approve();
        log.info("예약 신청 승인 완료 - applyId: {}", applyId);
    }

    /**
     * rejectApply - 예약 신청 반려 (관리자 전용)
     *
     * Apply.reject() 도메인 메서드로 상태를 REJECTED 로 변경합니다.
     */
    @Override
    @Transactional
    public void rejectApply(Long applyId) {
        log.info("예약 신청 반려 시작 - applyId: {}", applyId);

        Apply apply = applyRepository.findById(applyId)
                .orElseThrow(() -> new RuntimeException(
                        "예약 신청을 찾을 수 없습니다. id: " + applyId));

        // PENDING 상태만 반려 가능
        if (!apply.isPending()) {
            throw new IllegalStateException(
                    "대기 중인 신청만 반려할 수 있습니다. 현재 상태: " + apply.getStatus());
        }

        // 도메인 메서드로 상태 변경 → REJECTED
        apply.reject();
        log.info("예약 신청 반려 완료 - applyId: {}", applyId);
    }

    /**
     * getAllApplies - 전체 예약 신청 목록 조회 (관리자 전용)
     */
    @Override
    public Page<ApplyDTO> getAllApplies(Pageable pageable) {
        log.info("전체 예약 신청 목록 조회 - page: {}", pageable.getPageNumber());
        return applyRepository.findAll(pageable).map(ApplyDTO::fromEntity);
    }

    /**
     * createApplyAsAdmin - 관리자 직접 예약 등록
     * 중복 예약 검사를 수행한 뒤 Apply 엔티티를 저장합니다.
     */
    @Override
    @Transactional
    public Long createApplyAsAdmin(ApplyDTO dto, Long memberId) {
        log.info("관리자 예약 등록 시작 - memberId: {}, 시설: {}, 날짜: {}",
                memberId, dto.getFacilityType(), dto.getReserveDate());

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. id: " + memberId));

        boolean isDuplicate = applyRepository.existsByMemberIdAndReserveDateAndStatus(
                memberId, dto.getReserveDate(), "PENDING");
        if (isDuplicate) {
            throw new IllegalStateException(
                    "해당 날짜에 이미 예약 신청이 있습니다. 날짜: " + dto.getReserveDate());
        }

        Apply apply = Apply.builder()
                .member(member)
                .applicantName(dto.getApplicantName())
                .facilityType(dto.getFacilityType())
                .phone(dto.getPhone())
                .participants(dto.getParticipants())
                .reserveDate(dto.getReserveDate())
                .build();

        // 관리자가 등록 시 상태를 지정할 수 있음 (기본값: PENDING)
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            apply.changeStatus(dto.getStatus());
        }

        Long savedId = applyRepository.save(apply).getId();
        log.info("관리자 예약 등록 완료 - applyId: {}", savedId);
        return savedId;
    }

    /**
     * updateApply - 예약 정보 수정 (관리자 전용)
     * 정보 필드와 상태를 함께 변경합니다.
     */
    @Override
    @Transactional
    public void updateApply(Long id, ApplyDTO dto) {
        log.info("예약 정보 수정 시작 - applyId: {}", id);

        Apply apply = applyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "예약 신청을 찾을 수 없습니다. id: " + id));

        apply.updateInfo(
                dto.getApplicantName(),
                dto.getFacilityType(),
                dto.getPhone(),
                dto.getParticipants(),
                dto.getReserveDate());

        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            apply.changeStatus(dto.getStatus());
        }

        log.info("예약 정보 수정 완료 - applyId: {}", id);
    }

    /**
     * deleteApply - 예약 삭제 (관리자 전용)
     */
    @Override
    @Transactional
    public void deleteApply(Long id) {
        log.info("예약 삭제 시작 - applyId: {}", id);

        Apply apply = applyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "예약 신청을 찾을 수 없습니다. id: " + id));

        applyRepository.delete(apply);
        log.info("예약 삭제 완료 - applyId: {}", id);
    }
}
