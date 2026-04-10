package com.busanit501.api5012.service.library;

import com.busanit501.api5012.dto.library.InquiryDTO;
import com.busanit501.api5012.dto.library.ReplyDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * InquiryService - 문의사항 서비스 인터페이스
 *
 * 부산도서관 관리 시스템의 1:1 문의사항(Q&A) 비즈니스 로직을 정의합니다.
 * 비밀글 기능과 관리자 답변 기능을 지원합니다.
 *
 * [비밀글 처리 정책]
 * - secret = true 인 문의사항은 작성자 본인과 관리자만 내용(content)을 조회할 수 있습니다.
 * - 목록 조회 시 비밀글의 제목은 "비밀글입니다." 로 마스킹합니다.
 * - 컨트롤러 레이어에서 요청자의 회원 ID 를 함께 전달하여 권한을 확인합니다.
 *
 * [답변 처리 흐름]
 * 1. 관리자가 addReply() 호출 → Reply 엔티티 생성
 * 2. Inquiry.answered = true 로 자동 변경 (도메인 메서드 활용)
 */
public interface InquiryService {

    /**
     * createInquiry - 문의사항 작성
     *
     * 1. 회원 존재 여부 확인
     * 2. Inquiry 엔티티 생성
     *
     * @param dto      문의사항 정보 (제목, 내용, 비밀글 여부)
     * @param memberId 작성 회원 ID
     * @return 생성된 문의사항 ID
     */
    Long createInquiry(InquiryDTO dto, Long memberId);

    /**
     * getInquiries - 문의사항 목록 조회 (비밀글 처리 포함)
     *
     * 공개 문의사항 목록을 반환합니다.
     * 비밀글의 제목은 마스킹 처리합니다.
     * viewerMemberId 가 null 이면 관리자 요청으로 간주하여 비밀글도 반환합니다.
     *
     * @param pageable       페이지 정보
     * @param viewerMemberId 현재 요청자의 회원 ID (null 이면 관리자)
     * @return 페이지네이션이 적용된 문의사항 목록
     */
    Page<InquiryDTO> getInquiries(Pageable pageable, Long viewerMemberId);

    /**
     * getMyInquiries - 내 문의사항 목록 조회
     *
     * 마이페이지에서 내가 작성한 문의사항 목록을 조회합니다.
     * 비밀글도 본인이 작성한 것이므로 모두 포함합니다.
     *
     * @param memberId 조회할 회원 ID
     * @return 해당 회원의 전체 문의사항 목록
     */
    List<InquiryDTO> getMyInquiries(Long memberId);

    /**
     * getInquiryById - 문의사항 상세 조회 (답변 포함)
     *
     * JOIN FETCH 를 사용하여 답변 목록도 함께 로딩합니다.
     * 비밀글인 경우 viewerMemberId 가 작성자 ID 와 일치하거나 null(관리자)이어야 합니다.
     *
     * @param id             조회할 문의사항 ID
     * @param viewerMemberId 현재 요청자의 회원 ID (null 이면 관리자)
     * @return 답변 목록이 포함된 문의사항 상세 DTO
     * @throws RuntimeException    문의사항 없음
     * @throws IllegalAccessException 비밀글 권한 없을 때
     */
    InquiryDTO getInquiryById(Long id, Long viewerMemberId) throws IllegalAccessException;

    /**
     * addReply - 문의사항 답변 작성 (관리자 전용)
     *
     * 1. 문의사항 조회
     * 2. Reply 엔티티 생성
     * 3. Inquiry.addReply() 호출 → answered = true 자동 처리
     *
     * @param inquiryId 답변할 문의사항 ID
     * @param replyDTO  답변 정보 (내용, 답변자)
     * @return 생성된 답변 ID
     * @throws RuntimeException 문의사항 없음
     */
    Long addReply(Long inquiryId, ReplyDTO replyDTO);

    /**
     * updateInquiry - 문의사항 수정 (작성자 또는 관리자)
     * 제목/내용/비밀글 여부를 수정합니다.
     *
     * @param id  수정할 문의사항 기본키
     * @param dto 수정할 정보
     */
    void updateInquiry(Long id, InquiryDTO dto);

    /**
     * deleteInquiry - 문의사항 삭제 (작성자 또는 관리자)
     * cascade = ALL, orphanRemoval 설정으로 관련 답변도 함께 삭제됩니다.
     *
     * @param id 삭제할 문의사항 기본키
     */
    void deleteInquiry(Long id);
}
