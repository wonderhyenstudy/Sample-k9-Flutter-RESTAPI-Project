package com.busanit501.api5012.controller.library;

import com.busanit501.api5012.dto.library.ApplyDTO;
import com.busanit501.api5012.service.library.ApplyService;
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

import java.util.Map;

/**
 * ApplyController - 시설 예약 REST 컨트롤러
 *
 * 부산도서관 관리 시스템의 시설 예약 관련 API 엔드포인트를 제공합니다.
 * 기본 경로: /api/apply/
 *
 * [엔드포인트 목록]
 * POST /api/apply              - 시설 예약 신청
 * GET  /api/apply/my           - 내 예약 목록 조회
 * PUT  /api/apply/{id}/approve - 예약 승인 (관리자)
 * PUT  /api/apply/{id}/reject  - 예약 거절 (관리자)
 *
 * [예약 처리 흐름]
 * 1. POST /api/apply              → 예약 신청 (PENDING 상태)
 * 2. PUT  /api/apply/{id}/approve → 관리자 승인 (APPROVED 상태)
 * 3. PUT  /api/apply/{id}/reject  → 관리자 거절 (REJECTED 상태)
 *
 * [중복 예약 방지]
 * 같은 날짜에 동일 회원의 중복 예약 신청을 허용하지 않습니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/apply")
@RequiredArgsConstructor
@Tag(name = "시설 예약 API", description = "시설 예약 신청, 내 예약 목록 조회, 승인/거절 API")
public class ApplyController {

    /** ApplyService - 시설 예약 비즈니스 로직 서비스 */
    private final ApplyService applyService;

    // ──────────────────────────────────────────────────────
    // POST /api/apply  →  시설 예약 신청
    // ──────────────────────────────────────────────────────

    /**
     * createApply - 시설 예약 신청 API
     *
     * 회원이 특정 날짜에 시설을 예약 신청합니다.
     * 같은 날짜의 중복 신청은 허용하지 않습니다.
     *
     * 요청 JSON 예시:
     * {
     *   "memberId": 1,
     *   "facilityType": "세미나실",
     *   "reserveDate": "2024-06-15",
     *   "applicantName": "홍길동",
     *   "phone": "010-1234-5678",
     *   "participants": 10
     * }
     *
     * @param memberId 신청 회원 ID (쿼리 파라미터)
     * @param dto      예약 신청 정보
     * @return 201 Created + { "applyId": 1 }
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "시설 예약 신청", description = "회원이 시설을 예약 신청합니다. 같은 날짜·시설 중복 신청은 불가합니다.")
    public ResponseEntity<Map<String, Object>> createApply(
            @Parameter(description = "신청 회원 ID")
            @RequestParam Long memberId,
            @RequestBody ApplyDTO dto) {
        log.info("시설 예약 신청 요청 - memberId: {}, 날짜: {}", memberId, dto.getReserveDate());

        try {
            Long applyId = applyService.createApply(dto, memberId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "result", "success",
                            "applyId", applyId,
                            "message", "시설 예약 신청이 완료되었습니다."
                    ));
        } catch (IllegalStateException e) {
            // 중복 예약, 날짜 충돌 등
            log.warn("시설 예약 신청 실패 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("시설 예약 신청 오류 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // GET /api/apply/my?memberId={}  →  내 예약 목록 조회
    // ──────────────────────────────────────────────────────

    /**
     * getMyApplies - 내 예약 목록 조회 API
     *
     * 특정 회원의 시설 예약 이력을 페이지 단위로 반환합니다.
     * 예약 날짜 기준 내림차순으로 반환합니다.
     *
     * 요청 예시: GET /api/apply/my?memberId=1&page=0&size=10
     *
     * @param memberId 조회할 회원 ID
     * @param page     페이지 번호 (0부터 시작)
     * @param size     페이지 크기
     * @return 200 OK + Page<ApplyDTO>
     */
    @GetMapping("/my")
    @Operation(summary = "내 예약 목록 조회", description = "회원의 시설 예약 이력을 페이지 단위로 반환합니다.")
    public ResponseEntity<Page<ApplyDTO>> getMyApplies(
            @Parameter(description = "조회할 회원 ID")
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("내 예약 목록 조회 - memberId: {}, page: {}", memberId, page);

        Pageable pageable = PageRequest.of(page, size, Sort.by("reserveDate").descending());
        Page<ApplyDTO> applies = applyService.getMyApplies(memberId, pageable);
        return ResponseEntity.ok(applies);
    }

    // ──────────────────────────────────────────────────────
    // PUT /api/apply/{id}/approve  →  예약 승인 (관리자)
    // ──────────────────────────────────────────────────────

    /**
     * approveApply - 시설 예약 승인 API (관리자 전용)
     *
     * PENDING 상태의 예약을 APPROVED 로 변경합니다.
     * 이미 처리된 예약(APPROVED/REJECTED)은 승인할 수 없습니다.
     *
     * @param id 승인할 예약 기본키
     * @return 200 OK + { "result": "success" }
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "예약 승인 (관리자)", description = "PENDING 상태의 예약을 APPROVED로 변경합니다.")
    public ResponseEntity<Map<String, String>> approveApply(
            @Parameter(description = "승인할 예약 기본키")
            @PathVariable Long id) {
        log.info("시설 예약 승인 요청 - applyId: {}", id);

        try {
            applyService.approveApply(id);
            return ResponseEntity.ok(Map.of("result", "success", "message", "예약이 승인되었습니다."));
        } catch (IllegalStateException e) {
            // 이미 처리된 예약
            log.warn("예약 승인 불가 - applyId: {}, 사유: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("예약 승인 실패 - applyId: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // PUT /api/apply/{id}/reject  →  예약 거절 (관리자)
    // ──────────────────────────────────────────────────────

    /**
     * rejectApply - 시설 예약 거절 API (관리자 전용)
     *
     * PENDING 상태의 예약을 REJECTED 로 변경합니다.
     * 이미 처리된 예약(APPROVED/REJECTED)은 거절할 수 없습니다.
     *
     * @param id 거절할 예약 기본키
     * @return 200 OK + { "result": "success" }
     */
    @PutMapping("/{id}/reject")
    @Operation(summary = "예약 거절 (관리자)", description = "PENDING 상태의 예약을 REJECTED로 변경합니다.")
    public ResponseEntity<Map<String, String>> rejectApply(
            @Parameter(description = "거절할 예약 기본키")
            @PathVariable Long id) {
        log.info("시설 예약 거절 요청 - applyId: {}", id);

        try {
            applyService.rejectApply(id);
            return ResponseEntity.ok(Map.of("result", "success", "message", "예약이 거절되었습니다."));
        } catch (IllegalStateException e) {
            // 이미 처리된 예약
            log.warn("예약 거절 불가 - applyId: {}, 사유: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("예약 거절 실패 - applyId: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // GET /api/apply  →  전체 예약 목록 조회 (관리자)
    // ──────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "전체 예약 목록 조회 (관리자)", description = "모든 회원의 시설 예약 목록을 페이지 단위로 반환합니다.")
    public ResponseEntity<Page<ApplyDTO>> getAllApplies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("전체 예약 목록 조회 - page: {}", page);

        Pageable pageable = PageRequest.of(page, size, Sort.by("regDate").descending());
        Page<ApplyDTO> applies = applyService.getAllApplies(pageable);
        return ResponseEntity.ok(applies);
    }

    // ──────────────────────────────────────────────────────
    // POST /api/apply/admin  →  관리자 예약 등록
    // ──────────────────────────────────────────────────────

    @PostMapping(value = "/admin", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "관리자 예약 등록", description = "관리자가 특정 회원을 대신해 예약을 등록합니다.")
    public ResponseEntity<Map<String, Object>> createApplyAsAdmin(
            @Parameter(description = "대상 회원 ID")
            @RequestParam Long memberId,
            @RequestBody ApplyDTO dto) {
        log.info("관리자 예약 등록 요청 - memberId: {}, 날짜: {}", memberId, dto.getReserveDate());

        try {
            Long applyId = applyService.createApplyAsAdmin(dto, memberId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "result", "success",
                            "applyId", applyId,
                            "message", "예약이 등록되었습니다."
                    ));
        } catch (IllegalStateException e) {
            log.warn("관리자 예약 등록 실패 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("관리자 예약 등록 오류 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // PUT /api/apply/{id}  →  예약 수정 (관리자)
    // ──────────────────────────────────────────────────────

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "예약 정보 수정 (관리자)", description = "예약 정보(신청자명/시설/연락처/인원/날짜/상태)를 수정합니다.")
    public ResponseEntity<Map<String, String>> updateApply(
            @PathVariable Long id,
            @RequestBody ApplyDTO dto) {
        log.info("예약 정보 수정 요청 - applyId: {}", id);

        try {
            applyService.updateApply(id, dto);
            return ResponseEntity.ok(Map.of("result", "success", "message", "예약 정보가 수정되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // DELETE /api/apply/{id}  →  예약 삭제 (관리자)
    // ──────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "예약 삭제 (관리자)", description = "예약 신청을 삭제합니다.")
    public ResponseEntity<Map<String, String>> deleteApply(@PathVariable Long id) {
        log.info("예약 삭제 요청 - applyId: {}", id);

        try {
            applyService.deleteApply(id);
            return ResponseEntity.ok(Map.of("result", "success", "message", "예약이 삭제되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }
}
