package com.busanit501.api5012.controller.library;

import com.busanit501.api5012.dto.library.BookDTO;
import com.busanit501.api5012.service.library.BookService;
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
 * BookController - 도서 REST 컨트롤러
 *
 * 부산도서관 관리 시스템의 도서 관련 API 엔드포인트를 제공합니다.
 * 기본 경로: /api/book/
 *
 * [엔드포인트 목록]
 * GET    /api/book             - 도서 검색 (키워드, 페이지네이션)
 * GET    /api/book/{id}        - 도서 상세 조회
 * POST   /api/book             - 도서 등록 (관리자 전용)
 * PUT    /api/book/{id}        - 도서 수정 (관리자 전용)
 * DELETE /api/book/{id}        - 도서 삭제 (관리자 전용)
 * GET    /api/book/available   - 대여 가능 도서 목록
 *
 * [Pageable 파라미터]
 * Spring Data Web 이 쿼리 파라미터를 자동으로 Pageable 객체로 변환합니다.
 * 직접 PageRequest 를 생성하거나, @PageableDefault 를 사용할 수도 있습니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
@Tag(name = "도서 관리 API", description = "도서 검색, 상세 조회, 등록/수정/삭제 API")
public class BookController {

    /** BookService - 도서 비즈니스 로직 서비스 */
    private final BookService bookService;

    // ──────────────────────────────────────────────────────
    // GET /api/book  →  도서 검색 목록
    // ──────────────────────────────────────────────────────

    /**
     * getBooks - 도서 목록 조회 (키워드 검색 + 페이지네이션)
     *
     * keyword 가 없으면 전체 도서를 최신 등록순으로 반환합니다.
     * keyword 가 있으면 도서명/저자명/출판사명 통합 검색을 수행합니다.
     *
     * 요청 예시:
     *   GET /api/book?page=0&size=10
     *   GET /api/book?keyword=스프링&page=0&size=10
     *
     * [PageRequest.of() 설명]
     * PageRequest.of(page, size, sort) 로 Pageable 객체를 직접 생성합니다.
     * Sort.by("regDate").descending() : 등록일시 내림차순 정렬
     *
     * @param keyword 검색 키워드 (선택)
     * @param page    페이지 번호 (0부터 시작, 기본값 0)
     * @param size    페이지 크기 (기본값 10)
     * @return 200 OK + Page<BookDTO>
     */
    @GetMapping
    @Operation(summary = "도서 목록 조회", description = "키워드로 도서를 검색하고 페이지 단위로 반환합니다.")
    public ResponseEntity<Page<BookDTO>> getBooks(
            @Parameter(description = "검색 키워드 (도서명/저자명/출판사명)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "10") int size) {
        log.info("도서 목록 조회 요청 - keyword: {}, page: {}, size: {}", keyword, page, size);

        // PageRequest.of(페이지번호, 크기, 정렬) 로 Pageable 생성
        Pageable pageable = PageRequest.of(page, size, Sort.by("regDate").descending());
        Page<BookDTO> bookPage = bookService.getBooks(keyword, pageable);

        return ResponseEntity.ok(bookPage);
    }

    // ──────────────────────────────────────────────────────
    // GET /api/book/available  →  대여 가능 도서 목록
    // ──────────────────────────────────────────────────────

    /**
     * getAvailableBooks - 대여 가능 도서 목록 조회
     *
     * BookStatus.AVAILABLE 인 도서만 반환합니다.
     * 대여 신청 화면에서 사용합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 200 OK + Page<BookDTO>
     */
    @GetMapping("/available")
    @Operation(summary = "대여 가능 도서 목록", description = "현재 대여 가능한 도서만 조회합니다.")
    public ResponseEntity<Page<BookDTO>> getAvailableBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("대여 가능 도서 목록 조회 - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<BookDTO> bookPage = bookService.getAvailableBooks(pageable);
        return ResponseEntity.ok(bookPage);
    }

    // ──────────────────────────────────────────────────────
    // GET /api/book/{id}  →  도서 상세 조회
    // ──────────────────────────────────────────────────────

    /**
     * getBookById - 도서 상세 조회
     *
     * @param id 도서 기본키 (@PathVariable 로 URL 경로에서 추출)
     * @return 200 OK + BookDTO, 404 Not Found
     */
    @GetMapping("/{id}")
    @Operation(summary = "도서 상세 조회", description = "도서 ID로 상세 정보를 조회합니다.")
    public ResponseEntity<Object> getBookById(
            @Parameter(description = "도서 기본키")
            @PathVariable Long id) {
        log.info("도서 상세 조회 요청 - bookId: {}", id);

        try {
            BookDTO bookDTO = bookService.getBookById(id);
            return ResponseEntity.ok(bookDTO);
        } catch (RuntimeException e) {
            log.warn("도서 상세 조회 실패 - bookId: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // POST /api/book  →  도서 등록 (관리자)
    // ──────────────────────────────────────────────────────

    /**
     * registerBook - 도서 등록 API (관리자 전용)
     *
     * 새 도서를 시스템에 등록합니다.
     * ISBN 중복 시 400 Bad Request 를 반환합니다.
     *
     * @param dto 등록할 도서 정보
     * @return 201 Created + { "bookId": 1 }
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "도서 등록 (관리자)", description = "새 도서를 등록합니다. ISBN 중복 검사가 적용됩니다.")
    public ResponseEntity<Map<String, Object>> registerBook(@RequestBody BookDTO dto) {
        log.info("도서 등록 요청 - 제목: {}", dto.getBookTitle());

        try {
            Long bookId = bookService.registerBook(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("result", "success", "bookId", bookId));
        } catch (IllegalArgumentException e) {
            log.warn("도서 등록 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // PUT /api/book/{id}  →  도서 수정 (관리자)
    // ──────────────────────────────────────────────────────

    /**
     * updateBook - 도서 수정 API (관리자 전용)
     *
     * @param id  수정할 도서 기본키
     * @param dto 수정할 도서 정보
     * @return 200 OK + { "result": "success" }
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "도서 수정 (관리자)", description = "도서 정보(제목, 저자, 출판사, 설명)를 수정합니다.")
    public ResponseEntity<Map<String, String>> updateBook(
            @PathVariable Long id,
            @RequestBody BookDTO dto) {
        log.info("도서 수정 요청 - bookId: {}", id);

        try {
            bookService.updateBook(id, dto);
            return ResponseEntity.ok(Map.of("result", "success", "message", "도서 정보가 수정되었습니다."));
        } catch (RuntimeException e) {
            log.warn("도서 수정 실패 - bookId: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // PATCH /api/book/{id}/status  →  도서 상태 변경 (관리자)
    // ──────────────────────────────────────────────────────

    /**
     * changeBookStatus - 도서 상태 수동 변경 (관리자 전용)
     *
     * 요청 JSON: { "status": "LOST" }
     * 허용값: AVAILABLE, RENTED, RESERVED, LOST
     *
     * @param id   변경할 도서 기본키
     * @param body { "status": "LOST" }
     * @return 200 OK + { "result": "success" }
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "도서 상태 변경 (관리자)", description = "도서 상태를 수동으로 변경합니다. (AVAILABLE/RENTED/RESERVED/LOST)")
    public ResponseEntity<Map<String, String>> changeBookStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        log.info("도서 상태 변경 요청 - bookId: {}, status: {}", id, body.get("status"));

        try {
            bookService.changeBookStatus(id, body.get("status"));
            return ResponseEntity.ok(Map.of("result", "success", "message", "도서 상태가 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "error", "message", "유효하지 않은 상태값입니다."));
        } catch (RuntimeException e) {
            log.warn("도서 상태 변경 실패 - bookId: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────
    // DELETE /api/book/{id}  →  도서 삭제 (관리자)
    // ──────────────────────────────────────────────────────

    /**
     * deleteBook - 도서 삭제 API (관리자 전용)
     *
     * 대여 중인 도서는 삭제할 수 없습니다.
     *
     * @param id 삭제할 도서 기본키
     * @return 200 OK + { "result": "success" }
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "도서 삭제 (관리자)", description = "도서를 삭제합니다. 대여 중인 도서는 삭제 불가합니다.")
    public ResponseEntity<Map<String, String>> deleteBook(@PathVariable Long id) {
        log.info("도서 삭제 요청 - bookId: {}", id);

        try {
            bookService.deleteBook(id);
            return ResponseEntity.ok(Map.of("result", "success", "message", "도서가 삭제되었습니다."));
        } catch (IllegalStateException e) {
            // 대여 중인 도서 삭제 시도
            log.warn("도서 삭제 불가 - bookId: {}, 사유: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("도서 삭제 실패 - bookId: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }
}
