package com.busanit501.api5012.controller.library;

import com.busanit501.api5012.dto.library.InquiryDTO;
import com.busanit501.api5012.dto.library.ReplyDTO;
import com.busanit501.api5012.service.library.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * InquiryController - 문의사항 REST 컨트롤러
 *
 * 부산도서관 관리 시스템의 문의사항 관련 API 엔드포인트를 제공합니다.
 * 기본 경로: /api/inquiry/
 *
 * [엔드포인트 목록]
 * POST /api/inquiry                - 문의사항 작성
 * GET  /api/inquiry                - 문의사항 목록 조회 (비밀글 처리 포함)
 * GET  /api/inquiry/my             - 내 문의사항 목록 조회
 * GET  /api/inquiry/{id}           - 문의사항 상세 조회 (답변 포함)
 * POST /api/inquiry/{id}/reply     - 답변 작성 (관리자)
 *
 * [비밀글 처리 정책]
 * - 목록 조회: viewerMemberId 가 없으면(관리자) 전체 조회
 *              viewerMemberId 가 있으면 공개 문의 + 본인 비밀 문의만 반환
 * - 상세 조회: 비밀글은 작성자 본인 또는 관리자만 조회 가능
 *              권한 없으면 403 Forbidden 반환
 *
 * [viewerMemberId 사용 방법]
 * - 일반 회원  : viewerMemberId 에 회원 ID 값을 전달 (Long)
 * - 관리자     : viewerMemberId 를 생략하면 null 로 처리 (전체 조회)
 */
@Slf4j
@RestController
@RequestMapping("/api/inquiry")
@RequiredArgsConstructor
@Tag(name = "문의사항 API", description = "문의사항 작성, 목록/상세 조회, 답변 작성 API")
public class InquiryController {

    /** InquiryService - 문의사항 비즈니스 로직 서비스 */
    private final InquiryService inquiryService;

    // ──────────────────────────────────────────────────────
    // POST /api/inquiry  →  문의사항 작성
    // ──────────────────────────────────────────────────────

    /**
     * createInquiry - 문의사항 작성 API
     *
     * 회원이 문의사항을 작성합니다.
     * secret=true 이면 비밀글로 등록됩니다.
     *
     * 요청 JSON 예시:
     * {
     *   "title": "대출 기간 관련 문의",
     *   "content": "대출 기간을 연장하려면 어떻게 해야 하나요?",
     *   "writer": "홍길동",
     *   "secret": false
     * }
     *
     * @param memberId 작성 회원 ID (쿼리 파라미터)
     * @param dto      문의사항 내용
     * @return 201 Created + { "inquiryId": 1 }
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "문의사항 작성", description = "회원이 문의사항을 작성합니다. secret=true이면 비밀글로 등록됩니다.")
    public ResponseEntity<Map<String, Object>> createInquiry(
            @Parameter(description = "작성 회원 ID")
            @RequestParam Long memberId,
            @RequestBody InquiryDTO dto) {
        log.info("문의사항 작성 요청 - memberId: {}, 제목: {}", memberId, dto.getTitle());

        try {
            Long inquiryId = inquiryService.createInquiry(dto, memberId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "result", "success",
                            "inquiryId", inquiryId,
                            "message", "문의사항이 등록되었습니다."
                    ));
        } catch (RuntimeException e) {
            log.warn("문의사항 작성 실패 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // GET /api/inquiry  →  문의사항 목록 조회
    // ──────────────────────────────────────────────────────

    /**
     * getInquiries - 문의사항 목록 조회 API (비밀글 처리 포함)
     *
     * viewerMemberId 가 없으면 관리자로 간주하여 전체 문의사항을 반환합니다.
     * viewerMemberId 가 있으면 공개 문의 + 본인 비밀 문의만 반환합니다.
     * 비밀글 제목은 "비밀글입니다." 로 마스킹됩니다.
     *
     * 요청 예시:
     *   GET /api/inquiry?page=0&size=10               (관리자)
     *   GET /api/inquiry?page=0&size=10&viewerMemberId=1  (일반 회원)
     *
     * @param page           페이지 번호 (0부터 시작)
     * @param size           페이지 크기
     * @param viewerMemberId 조회 회원 ID (선택, 없으면 관리자로 처리)
     * @return 200 OK + Page<InquiryDTO>
     */
    @GetMapping
    @Operation(summary = "문의사항 목록 조회", description = "비밀글 처리가 포함된 문의사항 목록을 반환합니다. viewerMemberId 없으면 관리자 모드(전체 조회).")
    public ResponseEntity<Page<InquiryDTO>> getInquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "조회 회원 ID (없으면 관리자로 처리)")
            @RequestParam(required = false) Long viewerMemberId) {
        log.info("문의사항 목록 조회 - page: {}, viewerMemberId: {}", page, viewerMemberId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("regDate").descending());
        Page<InquiryDTO> inquiryPage = inquiryService.getInquiries(pageable, viewerMemberId);
        return ResponseEntity.ok(inquiryPage);
    }

    // ──────────────────────────────────────────────────────
    // GET /api/inquiry/my?memberId={}  →  내 문의사항 목록 조회
    // ──────────────────────────────────────────────────────

    /**
     * getMyInquiries - 내 문의사항 목록 조회 API
     *
     * 본인이 작성한 모든 문의사항(비밀글 포함)을 마스킹 없이 반환합니다.
     *
     * 요청 예시: GET /api/inquiry/my?memberId=1
     *
     * @param memberId 조회할 회원 ID
     * @return 200 OK + List<InquiryDTO>
     */
    @GetMapping("/my")
    @Operation(summary = "내 문의사항 목록 조회", description = "본인이 작성한 모든 문의사항(비밀글 포함)을 반환합니다.")
    public ResponseEntity<List<InquiryDTO>> getMyInquiries(
            @Parameter(description = "조회할 회원 ID")
            @RequestParam Long memberId) {
        log.info("내 문의사항 목록 조회 - memberId: {}", memberId);

        List<InquiryDTO> inquiries = inquiryService.getMyInquiries(memberId);
        return ResponseEntity.ok(inquiries);
    }

    // ──────────────────────────────────────────────────────
    // GET /api/inquiry/{id}  →  문의사항 상세 조회
    // ──────────────────────────────────────────────────────

    /**
     * getInquiryById - 문의사항 상세 조회 API (답변 포함)
     *
     * 비밀글인 경우 작성자 본인(viewerMemberId == 작성자 ID) 또는
     * 관리자(viewerMemberId 없음)만 조회할 수 있습니다.
     * 권한 없는 접근은 403 Forbidden 으로 처리합니다.
     *
     * 요청 예시:
     *   GET /api/inquiry/1                       (관리자)
     *   GET /api/inquiry/1?viewerMemberId=2      (일반 회원)
     *
     * @param id             조회할 문의사항 기본키
     * @param viewerMemberId 조회 회원 ID (선택, 없으면 관리자로 처리)
     * @return 200 OK + InquiryDTO (replies 포함)
     */
    @GetMapping("/{id}")
    @Operation(summary = "문의사항 상세 조회", description = "답변 포함 문의사항 상세 정보. 비밀글은 작성자 본인 또는 관리자만 조회 가능.")
    public ResponseEntity<Object> getInquiryById(
            @Parameter(description = "문의사항 기본키")
            @PathVariable Long id,
            @Parameter(description = "조회 회원 ID (없으면 관리자로 처리)")
            @RequestParam(required = false) Long viewerMemberId) {
        log.info("문의사항 상세 조회 - inquiryId: {}, viewerMemberId: {}", id, viewerMemberId);

        try {
            InquiryDTO inquiryDTO = inquiryService.getInquiryById(id, viewerMemberId);
            return ResponseEntity.ok(inquiryDTO);
        } catch (IllegalAccessException e) {
            // 비밀글 권한 없음
            log.warn("비밀글 접근 권한 없음 - inquiryId: {}, viewerMemberId: {}", id, viewerMemberId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("문의사항 조회 실패 - inquiryId: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // POST /api/inquiry/{id}/reply  →  답변 작성 (관리자)
    // ──────────────────────────────────────────────────────

    /**
     * addReply - 문의사항 답변 작성 API (관리자 전용)
     *
     * 문의사항에 답변을 작성합니다.
     * 답변 작성 후 해당 문의사항의 answered 필드가 true 로 변경됩니다.
     *
     * 요청 JSON 예시:
     * {
     *   "replyText": "안녕하세요. 대출 연장은 도서관 앱에서 가능합니다.",
     *   "replier": "도서관 관리자"
     * }
     *
     * @param id       답변할 문의사항 기본키
     * @param replyDTO 답변 내용
     * @return 201 Created + { "replyId": 1 }
     */
    @PostMapping(value = "/{id}/reply", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "답변 작성 (관리자)", description = "문의사항에 답변을 작성합니다. 답변 후 answered=true로 변경됩니다.")
    public ResponseEntity<Map<String, Object>> addReply(
            @Parameter(description = "답변할 문의사항 기본키")
            @PathVariable Long id,
            @RequestBody ReplyDTO replyDTO) {
        log.info("문의사항 답변 작성 요청 - inquiryId: {}, 답변자: {}", id, replyDTO.getReplier());

        try {
            Long replyId = inquiryService.addReply(id, replyDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "result", "success",
                            "replyId", replyId,
                            "message", "답변이 등록되었습니다."
                    ));
        } catch (RuntimeException e) {
            log.warn("답변 작성 실패 - inquiryId: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // PUT /api/inquiry/{id}  →  문의사항 수정
    // ──────────────────────────────────────────────────────

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "문의사항 수정", description = "작성자 또는 관리자 전용: 제목/내용/비밀글 여부를 수정합니다.")
    public ResponseEntity<Map<String, String>> updateInquiry(
            @PathVariable Long id,
            @RequestBody InquiryDTO dto) {
        log.info("문의사항 수정 요청 - inquiryId: {}", id);
        try {
            inquiryService.updateInquiry(id, dto);
            return ResponseEntity.ok(Map.of("result", "success", "message", "문의사항이 수정되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // DELETE /api/inquiry/{id}  →  문의사항 삭제
    // ──────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "문의사항 삭제", description = "작성자 또는 관리자 전용: 문의사항과 관련 답변을 모두 삭제합니다.")
    public ResponseEntity<Map<String, String>> deleteInquiry(@PathVariable Long id) {
        log.info("문의사항 삭제 요청 - inquiryId: {}", id);
        try {
            inquiryService.deleteInquiry(id);
            return ResponseEntity.ok(Map.of("result", "success", "message", "문의사항이 삭제되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }
}
