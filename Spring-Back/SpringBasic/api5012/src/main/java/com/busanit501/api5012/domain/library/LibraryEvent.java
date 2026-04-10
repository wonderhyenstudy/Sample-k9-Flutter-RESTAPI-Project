package com.busanit501.api5012.domain.library;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * LibraryEvent - 도서관 행사(이벤트) 엔티티
 *
 * 도서관에서 개최하는 각종 문화 행사 정보를 저장하는 JPA 엔티티입니다.
 * 행사 신청(EventApplication)과 1:N 관계를 가집니다.
 *
 * 행사 유형 (category):
 *   - 문화행사 : 작가 강연, 북콘서트, 전시회 등
 *   - 주말극장 : 영화 상영, 공연 등
 *   - 강좌     : 독서 토론, 글쓰기 강좌, 외국어 강좌 등
 *
 * 참가 인원 관리:
 *   - maxParticipants    : 최대 참가 가능 인원
 *   - currentParticipants: 현재 신청 인원
 *   - 신청 시 currentParticipants 가 maxParticipants 에 도달하면 status 를 CLOSED 로 변경
 *
 * DB 테이블명: tbl_lib_event
 */
@Entity
@Table(name = "tbl_lib_event")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class LibraryEvent {

    /**
     * id - 기본키 (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * category - 행사 카테고리
     * 행사의 종류를 분류합니다.
     * 허용값: "문화행사", "주말극장", "강좌"
     * 향후 Enum 으로 리팩토링을 고려할 수 있습니다.
     */
    @Column(nullable = false, length = 50)
    private String category;

    /**
     * title - 행사 제목
     * 행사 목록 화면에서 표시되는 행사명입니다.
     * 예: "2024 부산 독서의 달 특별 강연"
     */
    @Column(nullable = false, length = 300)
    private String title;

    /**
     * content - 행사 상세 내용
     * 행사 일정, 장소, 내용 등 상세 설명입니다.
     * @Lob 으로 선언하여 길이 제한 없이 저장합니다.
     */
    @Lob
    @Column(nullable = false)
    private String content;

    /**
     * eventDate - 행사 개최일
     * 행사가 열리는 날짜입니다. (시간 정보는 content 에 포함)
     */
    @Column(nullable = false)
    private LocalDate eventDate;

    /**
     * place - 행사 장소
     * 행사가 개최되는 장소명입니다.
     * 예: "부산도서관 대강당", "2층 세미나실"
     */
    @Column(nullable = false, length = 200)
    private String place;

    /**
     * maxParticipants - 최대 참가 인원
     * 행사에 참가할 수 있는 최대 인원수입니다.
     * 0이면 인원 제한이 없음을 의미할 수 있습니다.
     */
    @Column(nullable = false)
    private int maxParticipants;

    /**
     * currentParticipants - 현재 참가 신청 인원
     * 현재까지 신청이 완료된 인원수입니다.
     * 행사 신청 시 +1, 신청 취소 시 -1 됩니다.
     * 기본값은 0입니다.
     */
    @Column(nullable = false)
    @Builder.Default
    private int currentParticipants = 0;

    /**
     * status - 행사 신청 상태
     * - "OPEN"   : 신청 가능 (기본값)
     * - "CLOSED" : 신청 마감 (정원 초과 또는 관리자가 수동 마감)
     * 향후 Enum 으로 리팩토링을 고려할 수 있습니다.
     */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String status = "OPEN";

    /**
     * regDate - 행사 등록일시
     * 관리자가 행사를 시스템에 등록한 날짜와 시간입니다.
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime regDate = LocalDateTime.now();

    // ──────────────────────────────────────────────
    // 비즈니스 메서드 (도메인 로직)
    // ──────────────────────────────────────────────

    /**
     * increaseParticipants - 참가 인원 증가 메서드
     * 행사 신청 시 호출합니다.
     * 최대 인원에 도달하면 상태를 자동으로 CLOSED 로 변경합니다.
     *
     * @throws IllegalStateException 이미 정원이 마감된 경우
     */
    public void increaseParticipants() {
        if (this.currentParticipants >= this.maxParticipants) {
            throw new IllegalStateException("행사 정원이 마감되었습니다.");
        }
        this.currentParticipants++;
        if (this.currentParticipants >= this.maxParticipants) {
            this.status = "CLOSED";
        }
    }

    /**
     * decreaseParticipants - 참가 인원 감소 메서드
     * 행사 신청 취소 시 호출합니다.
     * 인원이 줄어들면 CLOSED 상태를 OPEN 으로 되돌립니다.
     */
    public void decreaseParticipants() {
        if (this.currentParticipants > 0) {
            this.currentParticipants--;
        }
        if ("CLOSED".equals(this.status) && this.currentParticipants < this.maxParticipants) {
            this.status = "OPEN";
        }
    }

    /**
     * closeEvent - 행사 수동 마감 메서드
     * 관리자가 직접 신청을 마감할 때 사용합니다.
     */
    public void closeEvent() {
        this.status = "CLOSED";
    }

    /**
     * isOpen - 신청 가능 여부 확인 메서드
     *
     * @return 신청 가능하면 true, 마감이면 false
     */
    public boolean isOpen() {
        return "OPEN".equals(this.status)
                && this.currentParticipants < this.maxParticipants;
    }

    /**
     * updateInfo - 행사 정보 수정 (관리자 전용)
     * 제목/카테고리/내용/날짜/장소/최대인원 등을 한 번에 수정합니다.
     * status 와 currentParticipants 는 별도 메서드로만 변경합니다.
     */
    public void updateInfo(String category, String title, String content,
                           LocalDate eventDate, String place, int maxParticipants) {
        if (category != null) this.category = category;
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (eventDate != null) this.eventDate = eventDate;
        if (place != null) this.place = place;
        if (maxParticipants > 0) this.maxParticipants = maxParticipants;
    }

    /**
     * reopenEvent - 행사 재개 (관리자 전용)
     * CLOSED 상태를 OPEN 으로 되돌립니다.
     */
    public void reopenEvent() {
        this.status = "OPEN";
    }
}
