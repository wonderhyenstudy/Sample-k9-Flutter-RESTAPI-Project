package com.busanit501.api5012.domain.library;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Member - 도서관 회원 엔티티
 *
 * 부산도서관 관리 시스템에 가입한 회원 정보를 저장하는 JPA 엔티티입니다.
 * 기존 APIUser(JWT 인증용)와 별개로, 도서관 서비스 전용 회원 정보를 관리합니다.
 *
 * 연관관계:
 *   - Rental    (1:N) : 회원이 대여한 도서 목록
 *   - Apply     (1:N) : 회원이 신청한 시설 예약 목록
 *   - WishBook  (1:N) : 회원이 신청한 희망 도서 목록
 *   - Inquiry   (1:N) : 회원이 작성한 문의사항 목록
 *   - EventApplication (1:N) : 회원이 신청한 행사 목록
 *
 * DB 테이블명: tbl_lib_member
 */
@Entity
@Table(name = "tbl_lib_member")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {}) // 연관관계 필드가 추가되면 exclude 에 포함
public class Member {

    /**
     * id - 기본키 (Primary Key)
     * DB에서 자동으로 증가하는 숫자형 식별자입니다. (AUTO_INCREMENT)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * mid - 회원 아이디 (로그인 ID)
     * 중복 불가(unique), null 불가(nullable=false) 제약 조건이 적용됩니다.
     * 길이는 최대 50자로 제한합니다.
     */
    @Column(unique = true, nullable = false, length = 50)
    private String mid;

    /**
     * mpw - 비밀번호 (BCrypt 암호화 저장)
     * 절대로 평문(plain text)으로 저장하지 않습니다.
     * 서비스 레이어에서 BCryptPasswordEncoder 로 암호화 후 저장해야 합니다.
     */
    @Column(nullable = false)
    private String mpw;

    /**
     * mname - 회원 이름 (실명)
     * 도서 대여 시 대여자 이름으로 표시되는 필드입니다.
     */
    @Column(nullable = false, length = 50)
    private String mname;

    /**
     * email - 이메일 주소
     * 중복 불가(unique) 제약 조건이 적용됩니다.
     * 비밀번호 찾기, 이벤트 당첨 알림 등에 활용됩니다.
     */
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /**
     * region - 거주 지역
     * 예: "부산광역시 해운대구", "부산광역시 남구" 등
     * 지역별 도서관 서비스 통계에 활용됩니다.
     */
    @Column(length = 100)
    private String region;

    /**
     * role - 회원 역할 (권한)
     * MemberRole 열거형을 문자열(STRING) 형태로 DB에 저장합니다.
     * 기본값은 USER (일반 회원)입니다.
     * @EnumType.STRING 으로 저장하면 DB에 "USER" 또는 "ADMIN" 문자열로 저장되어
     * 가독성과 유지보수성이 향상됩니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private MemberRole role = MemberRole.USER;

    /**
     * profileImg - 프로필 이미지 파일명
     * 업로드된 프로필 이미지의 파일명(UUID + 확장자)을 저장합니다.
     * null 이면 기본 이미지를 사용합니다.
     * 예: "550e8400-e29b-41d4-a716-446655440000.jpg"
     */
    @Column(length = 255)
    private String profileImg;

    /**
     * regDate - 가입일시
     * 회원이 가입한 날짜와 시간을 기록합니다.
     * insertable=false, updatable=false 로 설정하면 자동 관리가 가능하지만,
     * 여기서는 서비스 레이어에서 직접 설정하는 방식을 사용합니다.
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime regDate = LocalDateTime.now();

    // ──────────────────────────────────────────────
    // 비즈니스 메서드 (도메인 로직)
    // ──────────────────────────────────────────────

    /**
     * changePassword - 비밀번호 변경 메서드
     * 서비스 레이어에서 BCrypt 암호화 후 이 메서드를 호출합니다.
     *
     * @param mpw BCrypt 로 암호화된 새 비밀번호
     */
    public void changePassword(String mpw) {
        this.mpw = mpw;
    }

    /**
     * changeEmail - 이메일 변경 메서드
     * 이메일 중복 확인 후 서비스 레이어에서 호출합니다.
     *
     * @param email 변경할 새 이메일 주소
     */
    public void changeEmail(String email) {
        this.email = email;
    }

    /**
     * changeRegion - 거주 지역 변경 메서드
     *
     * @param region 변경할 지역 정보
     */
    public void changeRegion(String region) {
        this.region = region;
    }

    /**
     * changeProfileImg - 프로필 이미지 변경 메서드
     * 이미지 업로드 후 저장된 파일명을 전달합니다.
     *
     * @param profileImg 업로드된 이미지 파일명
     */
    public void changeProfileImg(String profileImg) {
        this.profileImg = profileImg;
    }

    /**
     * promoteToAdmin - 관리자 권한 부여 메서드
     * 슈퍼 관리자가 특정 회원에게 관리자 권한을 부여할 때 사용합니다.
     */
    public void promoteToAdmin() {
        this.role = MemberRole.ADMIN;
    }

    /**
     * changeRole - 회원 역할 변경 (관리자 전용)
     * 관리자 편집 화면에서 USER/ADMIN 토글 시 사용합니다.
     */
    public void changeRole(MemberRole role) {
        this.role = role;
    }

    /**
     * changeMname - 회원 이름 변경 (관리자 전용)
     */
    public void changeMname(String mname) {
        this.mname = mname;
    }
}
