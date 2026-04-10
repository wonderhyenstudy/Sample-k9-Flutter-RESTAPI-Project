package com.busanit501.api5012.domain.library;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Apply - 시설 예약 신청 엔티티
 *
 * 도서관 내 시설(세미나실, 스터디룸, 강당 등)의 예약 신청 정보를 저장하는 JPA 엔티티입니다.
 * 관리자가 신청을 승인(APPROVED) 또는 반려(REJECTED)할 수 있습니다.
 *
 * 시설 유형 (facilityType):
 *   - "세미나실" : 소규모 발표 및 회의 공간
 *   - "스터디룸" : 그룹 학습 공간
 *   - "강당"     : 대규모 행사 및 강연 공간
 *
 * 예약 상태 흐름:
 *   PENDING(대기) → APPROVED(승인) : 관리자 승인
 *   PENDING(대기) → REJECTED(반려) : 관리자 반려
 *
 * DB 테이블명: tbl_lib_apply
 */
@Entity
@Table(name = "tbl_lib_apply")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "member") // 연관 엔티티 순환 참조 방지
public class Apply {

    /**
     * id - 기본키 (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * member - 예약 신청 회원 (다대일 연관관계)
     * 예약을 신청한 회원 정보를 참조합니다.
     * LAZY 로딩으로 성능을 최적화합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * applicantName - 신청자 이름
     * 실제 시설을 사용할 담당자의 이름입니다.
     * member.mname 과 다를 수 있습니다 (단체 예약 등).
     */
    @Column(nullable = false, length = 50)
    private String applicantName;

    /**
     * facilityType - 시설 유형
     * 예약하려는 시설의 종류입니다.
     * 허용값: "세미나실", "스터디룸", "강당"
     * 향후 Enum 으로 리팩토링을 고려할 수 있습니다.
     */
    @Column(nullable = false, length = 30)
    private String facilityType;

    /**
     * phone - 연락처 전화번호
     * 예약 확인 또는 변경 사항 연락 시 사용합니다.
     * 예: "010-1234-5678"
     */
    @Column(nullable = false, length = 20)
    private String phone;

    /**
     * participants - 사용 인원
     * 시설을 사용할 예정 인원수입니다.
     * 시설별 최대 수용 인원 초과 여부를 확인하는 데 사용됩니다.
     */
    @Column(nullable = false)
    private int participants;

    /**
     * reserveDate - 예약 희망일
     * 시설을 사용하려는 날짜입니다.
     * 중복 예약 방지를 위해 같은 날짜·같은 시설의 예약이 이미 있는지 확인해야 합니다.
     */
    @Column(nullable = false)
    private LocalDate reserveDate;

    /**
     * status - 예약 신청 상태
     * - "PENDING"  : 대기중 (신청 직후 기본값)
     * - "APPROVED" : 승인완료
     * - "REJECTED" : 반려
     * 향후 Enum 으로 리팩토링을 고려할 수 있습니다.
     */
    @Column(nullable = false, length = 15)
    @Builder.Default
    private String status = "PENDING";

    /**
     * regDate - 신청 등록일시
     * 예약 신청이 등록된 날짜와 시간입니다.
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime regDate = LocalDateTime.now();

    // ──────────────────────────────────────────────
    // 비즈니스 메서드 (도메인 로직)
    // ──────────────────────────────────────────────

    /**
     * approve - 예약 승인 메서드
     * 관리자가 예약 신청을 승인할 때 호출합니다.
     * 상태를 APPROVED 로 변경합니다.
     */
    public void approve() {
        this.status = "APPROVED";
    }

    /**
     * reject - 예약 반려 메서드
     * 관리자가 예약 신청을 반려할 때 호출합니다.
     * 상태를 REJECTED 로 변경합니다.
     */
    public void reject() {
        this.status = "REJECTED";
    }

    /**
     * isPending - 대기 상태 확인 메서드
     * 아직 처리되지 않은 신청인지 확인합니다.
     *
     * @return 대기 중이면 true
     */
    public boolean isPending() {
        return "PENDING".equals(this.status);
    }

    /**
     * updateInfo - 예약 정보 수정 (관리자 전용)
     * 신청자명/시설 유형/연락처/인원/예약일을 수정합니다.
     * 상태(status)는 별도 메서드(approve/reject)로만 변경합니다.
     */
    public void updateInfo(String applicantName, String facilityType, String phone,
                           int participants, LocalDate reserveDate) {
        if (applicantName != null) this.applicantName = applicantName;
        if (facilityType != null) this.facilityType = facilityType;
        if (phone != null) this.phone = phone;
        if (participants > 0) this.participants = participants;
        if (reserveDate != null) this.reserveDate = reserveDate;
    }

    /**
     * changeStatus - 예약 상태 직접 변경 (관리자 전용)
     * PENDING/APPROVED/REJECTED 값을 직접 지정할 때 사용합니다.
     */
    public void changeStatus(String status) {
        this.status = status;
    }
}
