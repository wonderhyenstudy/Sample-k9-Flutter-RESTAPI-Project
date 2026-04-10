package com.busanit501.api5012.domain.library;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Inquiry - 문의사항 엔티티
 *
 * 회원이 도서관에 남기는 문의사항(Q&A)을 저장하는 JPA 엔티티입니다.
 * 비밀글 기능과 관리자 답변 기능을 지원합니다.
 *
 * 연관관계:
 *   - Member (N:1) : 문의를 작성한 회원 (LAZY 로딩)
 *   - Reply  (1:N) : 이 문의에 달린 답변 목록
 *     cascade = ALL   : Inquiry 삭제 시 답변도 함께 삭제
 *     orphanRemoval   : 목록에서 제거된 답변은 DB에서도 자동 삭제
 *
 * DB 테이블명: tbl_lib_inquiry
 */
@Entity
@Table(name = "tbl_lib_inquiry")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"member", "replies"}) // 연관 엔티티 순환 참조 방지
public class Inquiry {

    /**
     * id - 기본키 (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * title - 문의 제목
     * 문의사항 목록에서 표시되는 제목입니다.
     */
    @Column(nullable = false, length = 300)
    private String title;

    /**
     * content - 문의 내용
     * 문의 내용을 자세히 기술합니다. @Lob 으로 선언하여 긴 내용도 저장합니다.
     */
    @Lob
    @Column(nullable = false)
    private String content;

    /**
     * writer - 작성자 이름
     * 문의를 작성한 회원의 이름 또는 닉네임입니다.
     * member.mname 을 복사하거나, 비회원 문의 시 직접 입력받을 수 있습니다.
     */
    @Column(nullable = false, length = 100)
    private String writer;

    /**
     * answered - 답변 완료 여부
     * 관리자가 답변을 달면 true 로 변경합니다.
     * 문의 목록에서 "답변완료" / "미답변" 표시에 사용합니다.
     * 기본값: false (미답변)
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean answered = false;

    /**
     * secret - 비밀글 여부
     * true 이면 작성자 본인과 관리자만 내용을 볼 수 있습니다.
     * 개인 정보가 포함된 민감한 문의 시 사용합니다.
     * 기본값: false (공개글)
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean secret = false;

    /**
     * member - 문의 작성 회원 (다대일 연관관계)
     * 비회원 문의를 허용하지 않는 경우 nullable = false 로 설정합니다.
     * 비회원 문의를 허용하는 경우 nullable = true (기본값) 로 유지합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    /**
     * replies - 답변 목록 (일대다 연관관계)
     * 이 문의에 달린 모든 답변 목록입니다.
     * 일반적으로 하나의 문의에 하나의 답변이 달리지만,
     * 추가 답변이나 수정 답변을 위해 List 로 관리합니다.
     *
     * mappedBy = "inquiry" : Reply 엔티티의 inquiry 필드가 연관관계 주인
     * cascade = ALL        : 문의 삭제 시 답변도 함께 삭제
     * orphanRemoval = true : 목록에서 제거된 답변은 DB에서도 자동 삭제
     */
    @OneToMany(mappedBy = "inquiry",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reply> replies = new ArrayList<>();

    /**
     * regDate - 문의 등록일시
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime regDate = LocalDateTime.now();

    // ──────────────────────────────────────────────
    // 비즈니스 메서드 (도메인 로직)
    // ──────────────────────────────────────────────

    /**
     * addReply - 답변 추가 메서드
     * 답변을 추가하면서 양방향 연관관계를 설정하고 answered 를 true 로 변경합니다.
     *
     * @param reply 추가할 Reply 객체
     */
    public void addReply(Reply reply) {
        reply.setInquiry(this); // 양방향 관계 설정
        this.replies.add(reply);
        this.answered = true;   // 답변이 달리면 자동으로 답변완료 처리
    }

    /**
     * changeTitle - 제목 수정 메서드
     *
     * @param title 새 문의 제목
     */
    public void changeTitle(String title) {
        this.title = title;
    }

    /**
     * changeContent - 내용 수정 메서드
     *
     * @param content 새 문의 내용
     */
    public void changeContent(String content) {
        this.content = content;
    }

    /**
     * toggleSecret - 비밀글 토글 메서드
     * 비밀글을 공개글로, 공개글을 비밀글로 전환합니다.
     */
    public void toggleSecret() {
        this.secret = !this.secret;
    }

    /**
     * changeSecret - 비밀글 여부를 명시적으로 설정
     */
    public void changeSecret(boolean secret) {
        this.secret = secret;
    }
}
