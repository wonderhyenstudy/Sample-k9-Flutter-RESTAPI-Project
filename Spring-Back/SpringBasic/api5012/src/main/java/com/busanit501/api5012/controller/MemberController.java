package com.busanit501.api5012.controller;

import com.busanit501.api5012.dto.library.MemberDTO;
import com.busanit501.api5012.dto.library.MemberSignupDTO;
import com.busanit501.api5012.service.library.MemberLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MemberController - 회원 REST 컨트롤러 (통합)
 *
 * 로그인(JWT 발급)은 /generateToken 엔드포인트(APILoginFilter)에서 처리됩니다.
 * 이 컨트롤러는 회원가입, 정보 조회/수정, 중복 확인을 담당합니다.
 *
 * 기본 경로: /api/member
 */
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "회원 관리 API", description = "회원가입, 정보 조회/수정, 중복 확인 API")
public class MemberController {

    private final MemberLibraryService memberLibraryService;

    // ── POST /api/member/signup  →  회원가입 ────────────────────────────

    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "회원가입", description = "새 회원을 등록합니다. 아이디/이메일 중복 체크 및 BCrypt 암호화가 적용됩니다.")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody MemberSignupDTO dto) {
        log.info("회원가입 요청 - mid: {}", dto.getMid());
        try {
            Long memberId = memberLibraryService.signup(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "result", "success",
                            "memberId", memberId,
                            "message", "회원가입이 완료되었습니다."
                    ));
        } catch (IllegalArgumentException e) {
            log.warn("회원가입 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ── GET /api/member/me?mid={mid}  →  회원 정보 조회 ─────────────────

    @GetMapping("/me")
    @Operation(summary = "회원 정보 조회", description = "아이디(mid)로 회원 정보를 조회합니다.")
    public ResponseEntity<Object> getMyInfo(
            @Parameter(description = "조회할 회원 아이디") @RequestParam String mid) {
        log.info("회원 정보 조회 요청 - mid: {}", mid);
        try {
            MemberDTO memberDTO = memberLibraryService.getMemberByMid(mid);
            return ResponseEntity.ok(memberDTO);
        } catch (RuntimeException e) {
            log.warn("회원 정보 조회 실패 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ── PUT /api/member/update  →  회원 정보 수정 ─────────────────────

    @PutMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "회원 정보 수정", description = "이메일, 거주 지역을 수정합니다.")
    public ResponseEntity<Map<String, String>> updateMember(@RequestBody MemberDTO dto) {
        log.info("회원 정보 수정 요청 - mid: {}", dto.getMid());
        try {
            memberLibraryService.updateMember(dto.getMid(), dto);
            return ResponseEntity.ok(Map.of("result", "success", "message", "회원 정보가 수정되었습니다."));
        } catch (IllegalArgumentException e) {
            log.warn("회원 정보 수정 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("회원 정보 수정 실패 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ── PUT /api/member/profile-image  →  프로필 이미지 변경 ─────────────
    // body: { mid, base64Image } → 파일 저장 후 UUID 파일명을 DB에 기록
    // body: { mid, profileImg }  → 파일명만 DB 업데이트 (레거시)

    @PutMapping(value = "/profile-image", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "프로필 이미지 변경",
               description = "base64Image 가 있으면 파일 저장 후 DB 업데이트, 없으면 profileImg 파일명만 업데이트합니다.")
    public ResponseEntity<Map<String, String>> updateProfileImage(
            @RequestBody Map<String, String> body) {
        String mid = body.get("mid");
        log.info("프로필 이미지 변경 요청 - mid: {}", mid);
        try {
            String base64Image = body.get("base64Image");
            if (base64Image != null && !base64Image.isBlank()) {
                // base64 이미지를 파일로 저장하고 DB 업데이트
                String savedFileName = memberLibraryService.saveProfileImageBase64(mid, base64Image);
                return ResponseEntity.ok(Map.of(
                        "result", "success",
                        "message", "프로필 이미지가 변경되었습니다.",
                        "profileImg", savedFileName));
            } else {
                // 파일명만 DB 업데이트
                String profileImg = body.get("profileImg");
                memberLibraryService.updateProfileImage(mid, profileImg);
                return ResponseEntity.ok(Map.of("result", "success", "message", "프로필 이미지가 변경되었습니다."));
            }
        } catch (RuntimeException e) {
            log.warn("프로필 이미지 변경 실패 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ── GET /api/member/check-mid?mid={mid}  →  아이디 중복 체크 ─────────

    @GetMapping("/check-mid")
    @Operation(summary = "아이디 중복 체크", description = "입력한 아이디의 사용 가능 여부를 반환합니다.")
    public ResponseEntity<Map<String, Boolean>> checkMid(
            @Parameter(description = "중복 확인할 아이디") @RequestParam String mid) {
        log.info("아이디 중복 체크 요청 - mid: {}", mid);
        boolean isDuplicate = memberLibraryService.checkDuplicateMid(mid);
        return ResponseEntity.ok(Map.of("available", !isDuplicate));
    }

    // ── GET /api/member/check-email?email={email}  →  이메일 중복 체크 ───

    @GetMapping("/check-email")
    @Operation(summary = "이메일 중복 체크", description = "입력한 이메일의 사용 가능 여부를 반환합니다.")
    public ResponseEntity<Map<String, Boolean>> checkEmail(
            @Parameter(description = "중복 확인할 이메일") @RequestParam String email) {
        log.info("이메일 중입 체크 요청 - email: {}", email);
        boolean isDuplicate = memberLibraryService.checkDuplicateEmail(email);
        return ResponseEntity.ok(Map.of("available", !isDuplicate));
    }

    // ── GET /api/member/list  →  전체 회원 목록 (관리자 전용) ─────────────

    @GetMapping("/list")
    @Operation(summary = "전체 회원 목록 조회", description = "관리자 전용: 전체 회원 목록을 반환합니다.")
    public ResponseEntity<List<MemberDTO>> getAllMembers() {
        log.info("관리자 - 전체 회원 목록 조회");
        return ResponseEntity.ok(memberLibraryService.getAllMembers());
    }

    // ── PUT /api/member/admin/{id}  →  관리자 회원 수정 ─────────────────

    @PutMapping(value = "/admin/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "관리자 회원 수정", description = "관리자 전용: 회원 ID 로 이름/이메일/지역/역할을 수정합니다.")
    public ResponseEntity<Map<String, String>> adminUpdateMember(
            @PathVariable Long id,
            @RequestBody MemberDTO dto) {
        log.info("관리자 회원 수정 요청 - id: {}", id);
        try {
            memberLibraryService.adminUpdateMember(id, dto);
            return ResponseEntity.ok(Map.of("result", "success", "message", "회원 정보가 수정되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "error", "message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }

    // ── DELETE /api/member/{id}  →  관리자 회원 삭제 ────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "관리자 회원 삭제", description = "관리자 전용: 회원 ID 로 회원을 삭제합니다.")
    public ResponseEntity<Map<String, String>> deleteMember(@PathVariable Long id) {
        log.info("관리자 회원 삭제 요청 - id: {}", id);
        try {
            memberLibraryService.deleteMember(id);
            return ResponseEntity.ok(Map.of("result", "success", "message", "회원이 삭제되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "error", "message", e.getMessage()));
        }
    }
}
