package com.busanit501.api5012.controller.library;

import com.busanit501.api5012.domain.library.RentalStatus;
import com.busanit501.api5012.dto.library.RentalDTO;
import com.busanit501.api5012.service.library.RentalService;
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
 * RentalController - 도서 대여 REST 컨트롤러
 *
 * 부산도서관 관리 시스템의 도서 대여 관련 API 엔드포인트를 제공합니다.
 * 기본 경로: /api/rental/
 *
 * [엔드포인트 목록]
 * POST /api/rental              - 도서 대여 신청 { memberId, bookId }
 * PUT  /api/rental/{id}/return  - 반납 처리
 * PUT  /api/rental/{id}/extend  - 연장 처리 (7일)
 * GET  /api/rental?memberId={}  - 내 대여 목록
 * GET  /api/rental/status       - 상태별 대여 목록
 *
 * [대여 흐름 요약]
 * 1. POST /api/rental        → 대여 (도서 상태: AVAILABLE → RENTED)
 * 2. PUT  /api/rental/{id}/return → 반납 (도서 상태: RENTED → AVAILABLE)
 * 3. PUT  /api/rental/{id}/extend → 반납기한 +7일
 */
@Slf4j
@RestController
@RequestMapping("/api/rental")
@RequiredArgsConstructor
@Tag(name = "도서 대여 API", description = "도서 대여 신청, 반납, 연장, 대여 목록 조회 API")
public class RentalController {

    /** RentalService - 대여 비즈니스 로직 서비스 */
    private final RentalService rentalService;

    // ──────────────────────────────────────────────────────
    // POST /api/rental  →  도서 대여 신청
    // ──────────────────────────────────────────────────────

    /**
     * rentBook - 도서 대여 신청 API
     *
     * 요청 JSON 예시:
     * {
     *   "memberId": 1,
     *   "bookId": 5
     * }
     *
     * [에러 처리]
     * - 도서 없음: 404
     * - 대여 불가 상태: 409 Conflict
     * - 대여 한도 초과: 409 Conflict
     * - 중복 대여: 409 Conflict
     *
     * @param body { "memberId": Long, "bookId": Long }
     * @return 201 Created + { "rentalId": 1 }
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "도서 대여 신청", description = "회원이 도서를 대여 신청합니다. 최대 3권, 14일 대여.")
    public ResponseEntity<Map<String, Object>> rentBook(@RequestBody Map<String, Long> body) {
        Long memberId = body.get("memberId");
        Long bookId = body.get("bookId");
        log.info("도서 대여 신청 - memberId: {}, bookId: {}", memberId, bookId);

        try {
            Long rentalId = rentalService.rentBook(memberId, bookId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "result", "success",
                            "rentalId", rentalId,
                            "message", "도서 대여가 완료되었습니다. 반납 기한: 14일"
                    ));
        } catch (IllegalStateException e) {
            // 대여 불가, 한도 초과, 중복 대여 등
            log.warn("도서 대여 실패 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("도서 대여 오류 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // PUT /api/rental/{id}/return  →  반납 처리
    // ──────────────────────────────────────────────────────

    /**
     * returnBook - 도서 반납 처리 API
     *
     * 반납 처리 후 도서 상태가 AVAILABLE 로 변경됩니다.
     *
     * @param id 반납할 대여 기록 ID (경로 변수)
     * @return 200 OK + { "result": "success" }
     */
    @PutMapping("/{id}/return")
    @Operation(summary = "도서 반납", description = "대여 기록 ID로 반납 처리합니다. 도서 상태가 AVAILABLE로 변경됩니다.")
    public ResponseEntity<Map<String, String>> returnBook(
            @Parameter(description = "반납할 대여 기록 ID")
            @PathVariable Long id) {
        log.info("도서 반납 처리 요청 - rentalId: {}", id);

        try {
            rentalService.returnBook(id);
            return ResponseEntity.ok(Map.of("result", "success", "message", "반납이 완료되었습니다."));
        } catch (IllegalStateException e) {
            log.warn("반납 처리 실패 - rentalId: {}, 사유: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("반납 처리 오류 - rentalId: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // PUT /api/rental/{id}/extend  →  연장 처리
    // ──────────────────────────────────────────────────────

    /**
     * extendRental - 반납 기한 연장 API (7일)
     *
     * @param id 연장할 대여 기록 ID
     * @return 200 OK + { "result": "success" }
     */
    @PutMapping("/{id}/extend")
    @Operation(summary = "대여 연장", description = "반납 기한을 7일 연장합니다.")
    public ResponseEntity<Map<String, String>> extendRental(@PathVariable Long id) {
        log.info("대여 연장 요청 - rentalId: {}", id);

        try {
            rentalService.extendRental(id);
            return ResponseEntity.ok(Map.of("result", "success", "message", "대여 기간이 7일 연장되었습니다."));
        } catch (IllegalStateException e) {
            log.warn("연장 실패 - rentalId: {}, 사유: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("연장 오류 - rentalId: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // GET /api/rental?memberId={}  →  내 대여 목록
    // ──────────────────────────────────────────────────────

    /**
     * getMyRentals - 내 대여 목록 조회 API
     *
     * 요청 예시: GET /api/rental?memberId=1&page=0&size=10
     *
     * @param memberId 조회할 회원 ID
     * @param page     페이지 번호
     * @param size     페이지 크기
     * @return 200 OK + Page<RentalDTO>
     */
    @GetMapping
    @Operation(summary = "내 대여 목록 조회", description = "회원의 전체 대여 이력을 페이지 단위로 반환합니다.")
    public ResponseEntity<Page<RentalDTO>> getMyRentals(
            @Parameter(description = "조회할 회원 ID")
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("내 대여 목록 조회 - memberId: {}, page: {}", memberId, page);

        Pageable pageable = PageRequest.of(page, size, Sort.by("rentalDate").descending());
        Page<RentalDTO> rentalPage = rentalService.getMyRentals(memberId, pageable);
        return ResponseEntity.ok(rentalPage);
    }

    // ──────────────────────────────────────────────────────
    // GET /api/rental/all  →  전체 대여 목록 (관리자용)
    // ──────────────────────────────────────────────────────

    /**
     * getAllRentals - 전체 대여 목록 조회 (관리자 전용)
     *
     * 요청 예시: GET /api/rental/all?page=0&size=50
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 200 OK + Page<RentalDTO>
     */
    @GetMapping("/all")
    @Operation(summary = "전체 대여 목록 (관리자)", description = "모든 회원의 대여 기록을 반환합니다.")
    public ResponseEntity<Page<RentalDTO>> getAllRentals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("관리자 - 전체 대여 목록 조회 page: {}", page);
        Pageable pageable = PageRequest.of(page, size, Sort.by("rentalDate").descending());
        return ResponseEntity.ok(rentalService.getAllRentals(pageable));
    }

    // ──────────────────────────────────────────────────────
    // GET /api/rental/active?bookId={}  →  도서의 활성 대여 기록
    // ──────────────────────────────────────────────────────

    /**
     * getActiveRentalByBookId - 특정 도서의 현재 활성 대여 기록 조회
     *
     * 요청 예시: GET /api/rental/active?bookId=5
     *
     * @param bookId 조회할 도서 ID
     * @return 200 OK + RentalDTO (대여 중 아니면 404)
     */
    @GetMapping("/active")
    @Operation(summary = "도서 활성 대여 조회", description = "현재 대여 중인 도서의 대여 기록을 반환합니다.")
    public ResponseEntity<Object> getActiveRentalByBookId(
            @Parameter(description = "도서 ID") @RequestParam Long bookId) {
        log.info("도서 활성 대여 조회 - bookId: {}", bookId);
        return rentalService.getActiveRentalByBookId(bookId)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("result", "none", "message", "현재 대여 중인 기록이 없습니다.")));
    }

    // ──────────────────────────────────────────────────────
    // GET /api/rental/status?memberId={}&status={}  →  상태별 대여 목록
    // ──────────────────────────────────────────────────────

    /**
     * getMyRentalsByStatus - 상태별 대여 목록 조회 API
     *
     * 요청 예시: GET /api/rental/status?memberId=1&status=RENTING
     * status 값: RENTING, RETURNED, OVERDUE, EXTENDED
     *
     * @param memberId 조회할 회원 ID
     * @param status   대여 상태 문자열 (RentalStatus enum 이름)
     * @return 200 OK + List<RentalDTO>
     */
    @GetMapping("/status")
    @Operation(summary = "상태별 대여 목록 조회", description = "RENTING/RETURNED/OVERDUE/EXTENDED 상태별 대여 목록을 반환합니다.")
    public ResponseEntity<Object> getMyRentalsByStatus(
            @RequestParam Long memberId,
            @RequestParam String status) {
        log.info("상태별 대여 목록 조회 - memberId: {}, status: {}", memberId, status);

        try {
            // 문자열로 전달된 상태값을 RentalStatus Enum 으로 변환합니다.
            // RentalStatus.valueOf() : 문자열과 일치하는 Enum 상수를 반환합니다.
            // 일치하는 값이 없으면 IllegalArgumentException 이 발생합니다.
            RentalStatus rentalStatus = RentalStatus.valueOf(status.toUpperCase());
            List<RentalDTO> rentals = rentalService.getMyRentalsByStatus(memberId, rentalStatus);
            return ResponseEntity.ok(rentals);
        } catch (IllegalArgumentException e) {
            log.warn("유효하지 않은 대여 상태값 - status: {}", status);
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "error",
                            "message", "유효하지 않은 상태값입니다. (RENTING/RETURNED/OVERDUE/EXTENDED)"));
        }
    }
}
