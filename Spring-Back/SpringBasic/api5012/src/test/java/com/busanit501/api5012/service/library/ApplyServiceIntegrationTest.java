package com.busanit501.api5012.service.library;

import com.busanit501.api5012.domain.library.Apply;
import com.busanit501.api5012.domain.library.Member;
import com.busanit501.api5012.domain.library.MemberRole;
import com.busanit501.api5012.dto.library.ApplyDTO;
import com.busanit501.api5012.repository.library.ApplyRepository;
import com.busanit501.api5012.repository.library.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ApplyServiceIntegrationTest - ApplyService 관리자 CRUD 시나리오 검증
 *
 * [검증 목적]
 * 관리자 시설예약 관리 화면에서 호출되는 4개 API 의 서비스 레이어 동작을 검증합니다.
 *   1. getAllApplies(Pageable)             - 전체 예약 목록 페이지네이션
 *   2. createApplyAsAdmin(dto, memberId)   - 관리자 직접 등록 (상태 지정 가능)
 *   3. updateApply(id, dto)                - 정보 + 상태 동시 수정
 *   4. deleteApply(id)                     - 예약 삭제
 *
 * [테스트 격리]
 * @Transactional 로 각 테스트 종료 후 DB 상태 자동 롤백.
 */
@SpringBootTest
@Transactional
class ApplyServiceIntegrationTest {

    @Autowired
    private ApplyService applyService;

    @Autowired
    private ApplyRepository applyRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Long memberId;

    @BeforeEach
    void setUp() {
        Member member = Member.builder()
                .mid("applyadmin_" + System.currentTimeMillis())
                .mpw("password123!")
                .mname("시설예약테스트회원")
                .email("applyadmin_" + System.currentTimeMillis() + "@test.com")
                .role(MemberRole.USER)
                .build();
        memberId = memberRepository.save(member).getId();
    }

    // ─────────────────────────────────────────────────────────────────
    // getAllApplies
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllApplies() - 전체 예약을 페이지 단위로 반환한다")
    void getAllApplies_returnsAllWithPagination() {
        // given - 동일 회원 + 서로 다른 날짜로 3건 등록
        applyService.createApplyAsAdmin(buildDto("신청자A", "세미나실",
                LocalDate.now().plusDays(1), null), memberId);
        applyService.createApplyAsAdmin(buildDto("신청자B", "스터디룸",
                LocalDate.now().plusDays(2), null), memberId);
        applyService.createApplyAsAdmin(buildDto("신청자C", "강당",
                LocalDate.now().plusDays(3), null), memberId);

        // when
        Pageable pageable = PageRequest.of(0, 200);
        Page<ApplyDTO> result = applyService.getAllApplies(pageable);

        // then - 방금 등록한 3건이 포함되어 있어야 함
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(result.getContent())
                .extracting(ApplyDTO::getApplicantName)
                .contains("신청자A", "신청자B", "신청자C");
    }

    // ─────────────────────────────────────────────────────────────────
    // createApplyAsAdmin
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createApplyAsAdmin() - 상태 미지정 시 PENDING 으로 저장된다")
    void createApplyAsAdmin_withoutStatus_defaultsToPending() {
        // given
        ApplyDTO dto = buildDto("관리자등록", "세미나실",
                LocalDate.now().plusDays(5), null);

        // when
        Long savedId = applyService.createApplyAsAdmin(dto, memberId);

        // then
        Apply saved = applyRepository.findById(savedId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getApplicantName()).isEqualTo("관리자등록");
        assertThat(saved.getFacilityType()).isEqualTo("세미나실");
        assertThat(saved.getMember().getId()).isEqualTo(memberId);
    }

    @Test
    @DisplayName("createApplyAsAdmin() - DTO 에 상태가 있으면 해당 상태로 저장된다")
    void createApplyAsAdmin_withStatus_appliesStatus() {
        // given - 관리자가 바로 승인 상태로 등록
        ApplyDTO dto = buildDto("즉시승인", "강당",
                LocalDate.now().plusDays(10), "APPROVED");

        // when
        Long savedId = applyService.createApplyAsAdmin(dto, memberId);

        // then
        Apply saved = applyRepository.findById(savedId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("createApplyAsAdmin() - 동일 날짜 PENDING 중복 등록은 거부된다")
    void createApplyAsAdmin_duplicatePending_throws() {
        // given - 같은 날짜 PENDING 한 건 먼저 등록
        LocalDate date = LocalDate.now().plusDays(20);
        applyService.createApplyAsAdmin(buildDto("first", "세미나실", date, null), memberId);

        // when & then - 같은 날짜로 또 PENDING 등록 시 예외
        ApplyDTO duplicate = buildDto("second", "세미나실", date, null);
        assertThatThrownBy(() -> applyService.createApplyAsAdmin(duplicate, memberId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 예약");
    }

    @Test
    @DisplayName("createApplyAsAdmin() - 존재하지 않는 회원 ID 는 예외를 던진다")
    void createApplyAsAdmin_memberNotFound_throws() {
        ApplyDTO dto = buildDto("없는회원", "세미나실",
                LocalDate.now().plusDays(30), null);

        assertThatThrownBy(() -> applyService.createApplyAsAdmin(dto, 99999999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("회원을 찾을 수 없습니다");
    }

    // ─────────────────────────────────────────────────────────────────
    // updateApply
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateApply() - 정보 필드와 상태를 함께 수정한다")
    void updateApply_updatesFieldsAndStatus() {
        // given
        Long applyId = applyService.createApplyAsAdmin(
                buildDto("수정전", "세미나실", LocalDate.now().plusDays(40), null), memberId);

        // when - 신청자명, 시설, 인원, 상태 모두 변경
        ApplyDTO update = ApplyDTO.builder()
                .applicantName("수정후")
                .facilityType("스터디룸")
                .phone("010-9999-9999")
                .participants(10)
                .reserveDate(LocalDate.now().plusDays(41))
                .status("APPROVED")
                .build();
        applyService.updateApply(applyId, update);

        // then
        Apply updated = applyRepository.findById(applyId).orElseThrow();
        assertThat(updated.getApplicantName()).isEqualTo("수정후");
        assertThat(updated.getFacilityType()).isEqualTo("스터디룸");
        assertThat(updated.getPhone()).isEqualTo("010-9999-9999");
        assertThat(updated.getParticipants()).isEqualTo(10);
        assertThat(updated.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("updateApply() - status 가 null 이면 기존 상태를 유지한다")
    void updateApply_nullStatus_keepsOriginal() {
        // given - PENDING 으로 등록
        Long applyId = applyService.createApplyAsAdmin(
                buildDto("상태유지", "강당", LocalDate.now().plusDays(50), null), memberId);

        // when - status 없이 이름만 변경
        ApplyDTO update = ApplyDTO.builder()
                .applicantName("이름만변경")
                .facilityType("강당")
                .phone("010-1111-2222")
                .participants(5)
                .reserveDate(LocalDate.now().plusDays(50))
                // status 없음
                .build();
        applyService.updateApply(applyId, update);

        // then - 상태는 PENDING 유지, 이름만 변경
        Apply updated = applyRepository.findById(applyId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("PENDING");
        assertThat(updated.getApplicantName()).isEqualTo("이름만변경");
    }

    @Test
    @DisplayName("updateApply() - 존재하지 않는 예약 ID 는 예외를 던진다")
    void updateApply_notFound_throws() {
        ApplyDTO update = buildDto("x", "세미나실",
                LocalDate.now().plusDays(60), "APPROVED");

        assertThatThrownBy(() -> applyService.updateApply(99999999L, update))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("예약 신청을 찾을 수 없습니다");
    }

    // ─────────────────────────────────────────────────────────────────
    // deleteApply
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteApply() - 예약을 DB 에서 완전히 삭제한다")
    void deleteApply_removesFromDb() {
        // given
        Long applyId = applyService.createApplyAsAdmin(
                buildDto("삭제대상", "세미나실", LocalDate.now().plusDays(70), null), memberId);
        assertThat(applyRepository.findById(applyId)).isPresent();

        // when
        applyService.deleteApply(applyId);

        // then
        assertThat(applyRepository.findById(applyId)).isEmpty();
    }

    @Test
    @DisplayName("deleteApply() - 존재하지 않는 예약 ID 는 예외를 던진다")
    void deleteApply_notFound_throws() {
        assertThatThrownBy(() -> applyService.deleteApply(99999999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("예약 신청을 찾을 수 없습니다");
    }

    // ─────────────────────────────────────────────────────────────────
    // 기존 승인/반려 플로우 검증 (관리자 화면의 approve/reject 버튼)
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("approveApply() - PENDING 예약을 APPROVED 로 변경한다")
    void approveApply_changesStatusToApproved() {
        Long applyId = applyService.createApplyAsAdmin(
                buildDto("승인대상", "강당", LocalDate.now().plusDays(80), null), memberId);

        applyService.approveApply(applyId);

        assertThat(applyRepository.findById(applyId).orElseThrow().getStatus())
                .isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("rejectApply() - PENDING 예약을 REJECTED 로 변경한다")
    void rejectApply_changesStatusToRejected() {
        Long applyId = applyService.createApplyAsAdmin(
                buildDto("반려대상", "스터디룸", LocalDate.now().plusDays(90), null), memberId);

        applyService.rejectApply(applyId);

        assertThat(applyRepository.findById(applyId).orElseThrow().getStatus())
                .isEqualTo("REJECTED");
    }

    // ─────────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────────

    private ApplyDTO buildDto(String applicantName, String facilityType,
                              LocalDate reserveDate, String status) {
        return ApplyDTO.builder()
                .applicantName(applicantName)
                .facilityType(facilityType)
                .phone("010-0000-0000")
                .participants(3)
                .reserveDate(reserveDate)
                .status(status)
                .build();
    }
}
