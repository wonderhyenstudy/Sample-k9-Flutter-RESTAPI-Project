package com.busanit501.api5012.controller.library;

import com.busanit501.api5012.dto.library.EventApplicationDTO;
import com.busanit501.api5012.dto.library.LibraryEventDTO;
import com.busanit501.api5012.service.library.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * EventController - 도서관 행사 REST 컨트롤러
 *
 * 부산도서관 관리 시스템의 행사 관련 API 엔드포인트를 제공합니다.
 * 기본 경로: /api/event/
 *
 * [엔드포인트 목록]
 * GET  /api/event                       - 행사 목록 (페이지네이션)
 * GET  /api/event/range                 - 기간별 행사 목록
 * GET  /api/event/{id}                  - 행사 상세 조회
 * POST /api/event/{id}/apply            - 행사 신청
 * DELETE /api/event/application/{id}   - 행사 신청 취소
 * GET  /api/event/my                    - 내 행사 신청 목록
 *
 * [행사 신청 흐름]
 * 1. GET /api/event          → 행사 목록 조회
 * 2. POST /api/event/{id}/apply → 행사 신청 (참가자 수 증가)
 * 3. DELETE /api/event/application/{id} → 신청 취소 (참가자 수 감소)
 */
@Slf4j
@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
@Tag(name = "도서관 행사 API", description = "행사 목록 조회, 기간별 조회, 신청/취소, 내 신청 목록 API")
public class EventController {

    /** EventService - 행사 비즈니스 로직 서비스 */
    private final EventService eventService;

    // ──────────────────────────────────────────────────────
    // GET /api/event  →  행사 목록 (페이지네이션)
    // ──────────────────────────────────────────────────────

    /**
     * getEvents - 행사 목록 조회 (페이지네이션)
     *
     * 행사 시작일 기준 내림차순으로 행사 목록을 반환합니다.
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 200 OK + Page<LibraryEventDTO>
     */
    @GetMapping
    @Operation(summary = "행사 목록 조회", description = "행사 시작일 기준 내림차순으로 행사 목록을 페이지 단위로 반환합니다.")
    public ResponseEntity<Page<LibraryEventDTO>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("행사 목록 조회 요청 - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").descending());
        Page<LibraryEventDTO> eventPage = eventService.getEvents(pageable);
        return ResponseEntity.ok(eventPage);
    }

    // ──────────────────────────────────────────────────────
    // GET /api/event/range  →  기간별 행사 목록
    // ──────────────────────────────────────────────────────

    /**
     * getEventsByDateRange - 기간별 행사 목록 조회
     *
     * 시작일(start)과 종료일(end) 사이의 행사를 반환합니다.
     * 날짜 형식: yyyy-MM-dd
     *
     * 요청 예시: GET /api/event/range?start=2024-01-01&end=2024-12-31
     *
     * @param start 조회 시작일 (yyyy-MM-dd)
     * @param end   조회 종료일 (yyyy-MM-dd)
     * @return 200 OK + List<LibraryEventDTO>
     */
    @GetMapping("/range")
    @Operation(summary = "기간별 행사 목록 조회", description = "start ~ end 날짜 범위 내의 행사 목록을 반환합니다.")
    public ResponseEntity<Object> getEventsByDateRange(
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.info("기간별 행사 목록 조회 - start: {}, end: {}", start, end);

        try {
            List<LibraryEventDTO> events = eventService.getEventsByDateRange(start, end);
            return ResponseEntity.ok(events);
        } catch (IllegalArgumentException e) {
            log.warn("기간별 행사 조회 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // GET /api/event/{id}  →  행사 상세 조회
    // ──────────────────────────────────────────────────────

    /**
     * getEventById - 행사 상세 조회
     *
     * @param id 조회할 행사 기본키
     * @return 200 OK + LibraryEventDTO, 404 Not Found
     */
    @GetMapping("/{id}")
    @Operation(summary = "행사 상세 조회", description = "행사 ID로 상세 정보를 조회합니다.")
    public ResponseEntity<Object> getEventById(
            @Parameter(description = "행사 기본키")
            @PathVariable Long id) {
        log.info("행사 상세 조회 요청 - eventId: {}", id);

        try {
            LibraryEventDTO eventDTO = eventService.getEventById(id);
            return ResponseEntity.ok(eventDTO);
        } catch (RuntimeException e) {
            log.warn("행사 상세 조회 실패 - eventId: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // POST /api/event/{id}/apply  →  행사 신청
    // ──────────────────────────────────────────────────────

    /**
     * applyEvent - 행사 신청 API
     *
     * 회원이 특정 행사에 참가 신청합니다.
     * 정원 초과 또는 중복 신청 시 409 Conflict 를 반환합니다.
     *
     * 요청 예시: POST /api/event/1/apply?memberId=2
     *
     * @param id       신청할 행사 기본키 (경로 변수)
     * @param memberId 신청 회원 ID (쿼리 파라미터)
     * @return 201 Created + { "applicationId": 1 }
     */
    @PostMapping(value = "/{id}/apply")
    @Operation(summary = "행사 신청", description = "회원이 행사에 참가 신청합니다. 정원 초과 및 중복 신청 시 409를 반환합니다.")
    public ResponseEntity<Map<String, Object>> applyEvent(
            @Parameter(description = "신청할 행사 기본키")
            @PathVariable Long id,
            @Parameter(description = "신청 회원 ID")
            @RequestParam Long memberId) {
        log.info("행사 신청 요청 - eventId: {}, memberId: {}", id, memberId);

        try {
            Long applicationId = eventService.applyEvent(id, memberId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "result", "success",
                            "applicationId", applicationId,
                            "message", "행사 신청이 완료되었습니다."
                    ));
        } catch (IllegalStateException e) {
            // 정원 초과, 중복 신청 등
            log.warn("행사 신청 실패 - eventId: {}, memberId: {}, 사유: {}", id, memberId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("행사 신청 오류 - eventId: {}, memberId: {}, 오류: {}", id, memberId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // DELETE /api/event/application/{id}  →  행사 신청 취소
    // ──────────────────────────────────────────────────────

    /**
     * cancelEventApplication - 행사 신청 취소 API
     *
     * 행사 신청을 취소합니다. 취소 시 행사 현재 참가자 수가 감소합니다.
     *
     * @param id 취소할 행사 신청 기본키 (경로 변수)
     * @return 200 OK + { "result": "success" }
     */
    @DeleteMapping("/application/{id}")
    @Operation(summary = "행사 신청 취소", description = "행사 신청을 취소합니다. 현재 참가자 수가 감소합니다.")
    public ResponseEntity<Map<String, String>> cancelEventApplication(
            @Parameter(description = "취소할 행사 신청 기본키")
            @PathVariable Long id) {
        log.info("행사 신청 취소 요청 - applicationId: {}", id);

        try {
            eventService.cancelEventApplication(id);
            return ResponseEntity.ok(Map.of("result", "success", "message", "행사 신청이 취소되었습니다."));
        } catch (IllegalStateException e) {
            log.warn("행사 취소 불가 - applicationId: {}, 사유: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("행사 취소 실패 - applicationId: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // GET /api/event/my?memberId={}  →  내 행사 신청 목록
    // ──────────────────────────────────────────────────────

    /**
     * getMyEventApplications - 내 행사 신청 목록 조회 API
     *
     * 특정 회원의 전체 행사 신청 이력을 반환합니다.
     *
     * 요청 예시: GET /api/event/my?memberId=1
     *
     * @param memberId 조회할 회원 ID
     * @return 200 OK + List<EventApplicationDTO>
     */
    @GetMapping("/my")
    @Operation(summary = "내 행사 신청 목록 조회", description = "회원의 전체 행사 신청 이력을 반환합니다.")
    public ResponseEntity<List<EventApplicationDTO>> getMyEventApplications(
            @Parameter(description = "조회할 회원 ID")
            @RequestParam Long memberId) {
        log.info("내 행사 신청 목록 조회 - memberId: {}", memberId);

        List<EventApplicationDTO> applications = eventService.getMyEventApplications(memberId);
        return ResponseEntity.ok(applications);
    }

    // ──────────────────────────────────────────────────────
    // POST /api/event  →  행사 등록 (관리자)
    // ──────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "행사 등록(관리자)", description = "관리자 전용: 새 행사를 등록합니다.")
    public ResponseEntity<Map<String, Object>> createEvent(@RequestBody LibraryEventDTO dto) {
        log.info("행사 등록 요청 - title: {}", dto.getTitle());
        try {
            Long id = eventService.createEvent(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("result", "success", "eventId", id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // PUT /api/event/{id}  →  행사 수정 (관리자)
    // ──────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "행사 수정(관리자)", description = "관리자 전용: 행사 정보를 수정합니다.")
    public ResponseEntity<Map<String, String>> updateEvent(
            @PathVariable Long id,
            @RequestBody LibraryEventDTO dto) {
        log.info("행사 수정 요청 - eventId: {}", id);
        try {
            eventService.updateEvent(id, dto);
            return ResponseEntity.ok(Map.of("result", "success", "message", "행사가 수정되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // DELETE /api/event/{id}  →  행사 삭제 (관리자)
    // ──────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "행사 삭제(관리자)", description = "관리자 전용: 행사를 삭제합니다.")
    public ResponseEntity<Map<String, String>> deleteEvent(@PathVariable Long id) {
        log.info("행사 삭제 요청 - eventId: {}", id);
        try {
            eventService.deleteEvent(id);
            return ResponseEntity.ok(Map.of("result", "success", "message", "행사가 삭제되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }
}
