package com.busanit501.api5012.service.library;

import com.busanit501.api5012.dto.library.MemberDTO;
import com.busanit501.api5012.dto.library.MemberSignupDTO;

import java.util.List;

/**
 * MemberLibraryService - 도서관 회원 서비스 인터페이스
 *
 * 부산도서관 관리 시스템의 회원 관련 비즈니스 로직을 정의합니다.
 * 기존 JWT 인증용 APIUser 와 별개로, 도서관 전용 회원(Member)을 관리합니다.
 *
 * [인터페이스 분리 원칙(ISP) 적용]
 * 클라이언트(컨트롤러)는 구현 클래스(MemberLibraryServiceImpl)에 직접 의존하지 않고,
 * 이 인터페이스에만 의존합니다. 덕분에 구현체를 교체해도 컨트롤러 코드는 변경되지 않습니다.
 *
 * [JWT 로그인 처리 안내]
 * 실제 JWT 토큰 발급은 기존 /generateToken 엔드포인트(APILoginFilter)에서 처리합니다.
 * 이 서비스의 로그인 관련 메서드는 회원 정보 검증만 담당합니다.
 */
public interface MemberLibraryService {

    /**
     * signup - 회원가입 처리
     *
     * 1. 아이디 중복 체크
     * 2. 이메일 중복 체크
     * 3. 비밀번호 확인(mpw == mpwConfirm) 검증
     * 4. BCrypt 암호화 후 Member 엔티티 저장
     *
     * @param dto 회원가입 요청 정보 (아이디, 비밀번호, 이름, 이메일, 지역)
     * @return 생성된 회원의 ID (기본키)
     * @throws IllegalArgumentException 아이디/이메일 중복, 비밀번호 불일치 시
     */
    Long signup(MemberSignupDTO dto);

    /**
     * getMemberByMid - 아이디로 회원 정보 조회
     *
     * 마이페이지, 프로필 조회 등에서 사용합니다.
     *
     * @param mid 조회할 회원 아이디
     * @return 회원 정보 DTO
     * @throws RuntimeException 해당 아이디의 회원이 없을 때
     */
    MemberDTO getMemberByMid(String mid);

    /**
     * updateMember - 회원 정보 수정
     *
     * 이메일, 지역 정보를 수정합니다.
     * 비밀번호 변경은 별도 메서드로 처리하는 것을 권장합니다.
     *
     * @param mid 수정할 회원 아이디
     * @param dto 수정할 회원 정보 (이메일, 지역)
     */
    void updateMember(String mid, MemberDTO dto);

    /**
     * updateProfileImage - 프로필 이미지 변경
     *
     * 이미지 업로드 후 저장된 파일명을 받아 DB를 갱신합니다.
     *
     * @param mid        수정할 회원 아이디
     * @param profileImg 업로드된 이미지 파일명
     */
    void updateProfileImage(String mid, String profileImg);

    /**
     * checkDuplicateMid - 아이디 중복 체크
     *
     * 회원가입 전 아이디 사용 가능 여부를 확인합니다.
     * true 이면 이미 사용 중인 아이디입니다.
     *
     * @param mid 중복 확인할 아이디
     * @return 중복이면 true, 사용 가능하면 false
     */
    boolean checkDuplicateMid(String mid);

    /**
     * checkDuplicateEmail - 이메일 중복 체크
     *
     * 회원가입 또는 이메일 변경 전 사용 가능 여부를 확인합니다.
     *
     * @param email 중복 확인할 이메일
     * @return 중복이면 true, 사용 가능하면 false
     */
    boolean checkDuplicateEmail(String email);

    /**
     * getAllMembers - 전체 회원 목록 조회 (관리자 전용)
     *
     * @return 전체 회원 DTO 목록
     */
    List<MemberDTO> getAllMembers();

    /**
     * saveProfileImageBase64 - base64 이미지를 저장하고 DB 업데이트
     *
     * @param mid         회원 아이디
     * @param base64Image base64 인코딩된 이미지 데이터
     * @return 저장된 UUID 파일명
     */
    String saveProfileImageBase64(String mid, String base64Image);

    /**
     * adminUpdateMember - 관리자 전용 회원 정보 수정
     * id 기반으로 이름/이메일/지역/역할을 수정합니다. (비밀번호 제외)
     *
     * @param id  수정할 회원 기본키
     * @param dto 수정할 회원 정보 (mname, email, region, role)
     */
    void adminUpdateMember(Long id, MemberDTO dto);

    /**
     * deleteMember - 관리자 전용 회원 삭제
     *
     * @param id 삭제할 회원 기본키
     */
    void deleteMember(Long id);
}
