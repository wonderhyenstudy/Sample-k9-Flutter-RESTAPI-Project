package com.busanit501.api5012.service.library;

import com.busanit501.api5012.domain.library.Inquiry;
import com.busanit501.api5012.domain.library.Member;
import com.busanit501.api5012.domain.library.Reply;
import com.busanit501.api5012.dto.library.InquiryDTO;
import com.busanit501.api5012.dto.library.ReplyDTO;
import com.busanit501.api5012.repository.library.InquiryRepository;
import com.busanit501.api5012.repository.library.MemberRepository;
import com.busanit501.api5012.repository.library.ReplyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * InquiryServiceImpl - 문의사항 서비스 구현체
 *
 * InquiryService 인터페이스의 구현 클래스입니다.
 * 문의사항 작성, 목록/상세 조회(비밀글 처리 포함), 관리자 답변 기능을 구현합니다.
 *
 * [비밀글 처리 정책]
 * - 목록 조회: viewerMemberId == null(관리자) 이면 모든 문의사항을 반환
 *              viewerMemberId 가 있으면 공개 문의 + 본인 비밀 문의를 반환
 * - 상세 조회: 비밀글인 경우 viewerMemberId 가 작성자 ID 와 일치하거나
 *              null(관리자)이어야 내용을 반환 (아니면 IllegalAccessException)
 *
 * [답변 처리]
 * Inquiry.addReply() 도메인 메서드로 Reply 를 추가하면
 * answered 필드가 자동으로 true 로 변경됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryServiceImpl implements InquiryService {

    /** InquiryRepository - 문의사항 엔티티에 대한 DB 접근 */
    private final InquiryRepository inquiryRepository;

    /** MemberRepository - 회원 엔티티에 대한 DB 접근 */
    private final MemberRepository memberRepository;

    /** ReplyRepository - 답변 엔티티에 대한 DB 접근 */
    private final ReplyRepository replyRepository;

    /**
     * createInquiry - 문의사항 작성
     *
     * 회원 정보를 조회하여 작성자명을 자동으로 설정합니다.
     */
    @Override
    @Transactional
    public Long createInquiry(InquiryDTO dto, Long memberId) {
        log.info("문의사항 작성 시작 - memberId: {}, 제목: {}", memberId, dto.getTitle());

        // 1단계: 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. id: " + memberId));

        // 2단계: Inquiry 엔티티 생성 (빌더 패턴)
        // 작성자명은 회원의 실명(mname)을 사용하거나, DTO 의 writer 를 우선 사용합니다.
        String writer = (dto.getWriter() != null && !dto.getWriter().isBlank())
                ? dto.getWriter()
                : member.getMname();

        Inquiry inquiry = Inquiry.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .writer(writer)
                .member(member)
                .secret(dto.isSecret())
                .build(); // answered 기본값: false, regDate 기본값: now()

        Long savedId = inquiryRepository.save(inquiry).getId();
        log.info("문의사항 작성 완료 - inquiryId: {}", savedId);

        return savedId;
    }

    /**
     * getInquiries - 문의사항 목록 조회 (비밀글 처리 포함)
     *
     * viewerMemberId 에 따라 반환 범위를 다르게 처리합니다.
     * - null (관리자) : 전체 문의사항
     * - 일반 회원 ID  : 공개 문의 + 본인 비밀 문의
     *
     * InquiryDTO.fromEntityForList() 로 비밀글 제목을 마스킹합니다.
     */
    @Override
    public Page<InquiryDTO> getInquiries(Pageable pageable, Long viewerMemberId) {
        log.info("문의사항 목록 조회 - page: {}, viewerMemberId: {}",
                pageable.getPageNumber(), viewerMemberId);

        if (viewerMemberId == null) {
            // 관리자: 전체 문의사항 조회 (비밀글 포함)
            Page<Inquiry> inquiryPage = inquiryRepository.findAll(pageable);
            // 관리자는 isOwnerOrAdmin = true → 마스킹 없이 반환
            return inquiryPage.map(inquiry ->
                    InquiryDTO.fromEntityForList(inquiry, true));
        }

        // 일반 회원: 공개 문의 + 본인이 작성한 비밀 문의 조회
        Page<Inquiry> inquiryPage = inquiryRepository.findInquiriesForMember(viewerMemberId, pageable);
        return inquiryPage.map(inquiry -> {
            // 본인이 작성한 문의인지 확인 (비밀글 마스킹 여부 결정)
            boolean isOwner = inquiry.getMember() != null
                    && inquiry.getMember().getId().equals(viewerMemberId);
            return InquiryDTO.fromEntityForList(inquiry, isOwner);
        });
    }

    /**
     * getMyInquiries - 내 문의사항 목록 조회
     *
     * 본인이 작성한 모든 문의사항(비밀글 포함)을 반환합니다.
     * 마스킹 없이 전체 내용을 볼 수 있습니다.
     */
    @Override
    public List<InquiryDTO> getMyInquiries(Long memberId) {
        log.info("내 문의사항 목록 조회 - memberId: {}", memberId);

        // 본인 문의사항 전체 조회 (비밀글 포함)
        List<Inquiry> inquiries = inquiryRepository.findByMemberId(memberId);

        return inquiries.stream()
                // 본인 조회이므로 isOwnerOrAdmin = true (마스킹 없음)
                .map(inquiry -> InquiryDTO.fromEntityForList(inquiry, true))
                .collect(Collectors.toList());
    }

    /**
     * getInquiryById - 문의사항 상세 조회 (답변 포함)
     *
     * [비밀글 권한 검사]
     * - 비밀글이 아닌 경우: 모두 조회 가능
     * - 비밀글인 경우:
     *   - viewerMemberId == null (관리자) → 조회 가능
     *   - viewerMemberId == 작성자 ID    → 조회 가능
     *   - 그 외                          → IllegalAccessException 발생
     *
     * findWithRepliesById() : JOIN FETCH 로 replies 컬렉션을 한 번에 로딩
     */
    @Override
    public InquiryDTO getInquiryById(Long id, Long viewerMemberId) throws IllegalAccessException {
        log.info("문의사항 상세 조회 - inquiryId: {}, viewerMemberId: {}", id, viewerMemberId);

        // JOIN FETCH 로 replies 한 번에 로딩
        Inquiry inquiry = inquiryRepository.findWithRepliesById(id)
                .orElseThrow(() -> new RuntimeException("문의사항을 찾을 수 없습니다. id: " + id));

        // 비밀글 권한 검사
        if (inquiry.isSecret()) {
            // viewerMemberId 가 null 이면 관리자로 간주하여 허용
            if (viewerMemberId != null) {
                // 작성자 본인인지 확인
                boolean isOwner = inquiry.getMember() != null
                        && inquiry.getMember().getId().equals(viewerMemberId);
                if (!isOwner) {
                    log.warn("비밀글 접근 권한 없음 - inquiryId: {}, viewerMemberId: {}",
                            id, viewerMemberId);
                    throw new IllegalAccessException(
                            "비밀글은 작성자 본인 또는 관리자만 조회할 수 있습니다.");
                }
            }
        }

        // 전체 내용(content + replies 포함) DTO 변환
        return InquiryDTO.fromEntity(inquiry);
    }

    /**
     * addReply - 문의사항 답변 작성 (관리자 전용)
     *
     * [처리 순서]
     * 1. 문의사항 조회
     * 2. Reply 엔티티 생성
     * 3. Inquiry.addReply() 도메인 메서드 호출
     *    → 양방향 관계 설정 + answered = true 자동 처리
     * 4. 더티체킹으로 Inquiry 자동 UPDATE
     *
     * [cascade = ALL 설명]
     * Inquiry 에 cascade = ALL 이 설정되어 있으므로
     * Inquiry 를 save() 하지 않아도 Reply 가 자동으로 DB에 저장됩니다.
     * 하지만 명시적으로 replyRepository.save() 를 호출하여 반환값을 얻습니다.
     */
    @Override
    @Transactional
    public Long addReply(Long inquiryId, ReplyDTO replyDTO) {
        log.info("답변 작성 시작 - inquiryId: {}, 답변자: {}", inquiryId, replyDTO.getReplier());

        // 1단계: 문의사항 조회 (replies 포함)
        Inquiry inquiry = inquiryRepository.findWithRepliesById(inquiryId)
                .orElseThrow(() -> new RuntimeException("문의사항을 찾을 수 없습니다. id: " + inquiryId));

        // 2단계: Reply 엔티티 생성
        Reply reply = Reply.builder()
                .replyText(replyDTO.getReplyText())
                .replier(replyDTO.getReplier())
                .build(); // inquiry 는 아래 addReply() 에서 설정됩니다.

        // 3단계: Inquiry.addReply() 호출
        // → reply.setInquiry(this) 양방향 관계 설정
        // → this.replies.add(reply) 컬렉션에 추가
        // → this.answered = true 자동 처리
        inquiry.addReply(reply);

        // 4단계: Reply 저장 (ID 를 반환하기 위해 명시적 저장)
        Long replyId = replyRepository.save(reply).getId();
        log.info("답변 작성 완료 - replyId: {}, inquiryId: {}", replyId, inquiryId);

        return replyId;
    }

    /**
     * updateInquiry - 문의사항 수정 (작성자 또는 관리자)
     * 제목/내용/비밀글 여부 필드를 변경합니다.
     */
    @Override
    @Transactional
    public void updateInquiry(Long id, InquiryDTO dto) {
        log.info("문의사항 수정 - inquiryId: {}", id);

        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문의사항을 찾을 수 없습니다. id: " + id));

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            inquiry.changeTitle(dto.getTitle());
        }
        if (dto.getContent() != null && !dto.getContent().isBlank()) {
            inquiry.changeContent(dto.getContent());
        }
        inquiry.changeSecret(dto.isSecret());

        log.info("문의사항 수정 완료 - inquiryId: {}", id);
    }

    /**
     * deleteInquiry - 문의사항 삭제
     * cascade = ALL + orphanRemoval 로 관련 답변도 함께 삭제됩니다.
     */
    @Override
    @Transactional
    public void deleteInquiry(Long id) {
        log.info("문의사항 삭제 - inquiryId: {}", id);

        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문의사항을 찾을 수 없습니다. id: " + id));

        inquiryRepository.delete(inquiry);
        log.info("문의사항 삭제 완료 - inquiryId: {}", id);
    }
}
