package com.busanit501.api5012.service.library;

import com.busanit501.api5012.dto.library.EventApplicationDTO;
import com.busanit501.api5012.dto.library.LibraryEventDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * EventService - 도서관 행사(이벤트) 서비스 인터페이스
 *
 * 부산도서관 관리 시스템의 행사 안내 및 참가 신청 비즈니스 로직을 정의합니다.
 *
 * [행사 신청 흐름]
 * 1. 행사 목록/상세 조회
 * 2. 신청 가능 여부 확인 (OPEN 상태, 정원 미달)
 * 3. 중복 신청 여부 확인
 * 4. applyEvent() 호출 → EventApplication 생성, LibraryEvent 참가 인원 +1
 * 5. 취소 시 cancelEventApplication() → 참가 인원 -1, 상태 OPEN 복구
 */
public interface EventService {

    /**
     * getEvents - 행사 목록 조회 (페이지네이션)
     *
     * 전체 행사를 최신 등록순으로 조회합니다.
     *
     * @param pageable 페이지 정보
     * @return 페이지네이션이 적용된 행사 목록
     */
    Page<LibraryEventDTO> getEvents(Pageable pageable);

    /**
     * getEventsByDateRange - 기간별 행사 조회
     *
     * 특정 기간(시작일 ~ 종료일) 에 열리는 행사 목록을 반환합니다.
     * 월별 행사 캘린더 화면에서 사용합니다.
     *
     * @param start 조회 시작일 (포함)
     * @param end   조회 종료일 (포함)
     * @return 해당 기간의 행사 목록
     */
    List<LibraryEventDTO> getEventsByDateRange(LocalDate start, LocalDate end);

    /**
     * getEventById - 행사 상세 조회
     *
     * @param id 조회할 행사 기본키
     * @return 행사 상세 정보 DTO (잔여 인원 포함)
     * @throws RuntimeException 해당 ID의 행사가 없을 때
     */
    LibraryEventDTO getEventById(Long id);

    /**
     * applyEvent - 행사 신청
     *
     * 1. 행사 존재 여부 확인
     * 2. 신청 가능 상태(OPEN) 확인
     * 3. 정원 초과 여부 확인
     * 4. 중복 신청 여부 확인
     * 5. EventApplication 생성
     * 6. LibraryEvent.currentParticipants +1 처리
     *
     * @param eventId  신청할 행사 ID
     * @param memberId 신청하는 회원 ID
     * @return 생성된 행사 신청 기록 ID
     * @throws IllegalStateException 신청 마감, 정원 초과, 중복 신청 시
     */
    Long applyEvent(Long eventId, Long memberId);

    /**
     * cancelEventApplication - 행사 신청 취소
     *
     * 1. 신청 기록 조회
     * 2. 신청 상태를 CANCELLED 로 변경
     * 3. LibraryEvent.currentParticipants -1 처리 (OPEN 복구 가능)
     *
     * @param applicationId 취소할 신청 기록 ID
     * @throws RuntimeException 신청 기록 없음, 이미 취소된 경우
     */
    void cancelEventApplication(Long applicationId);

    /**
     * getMyEventApplications - 내 행사 신청 목록 조회
     *
     * 마이페이지에서 내가 신청한 행사 이력을 조회합니다.
     * 행사 제목, 날짜, 장소 등의 정보를 함께 반환합니다.
     *
     * @param memberId 조회할 회원 ID
     * @return 회원의 전체 행사 신청 목록
     */
    List<EventApplicationDTO> getMyEventApplications(Long memberId);

    /**
     * createEvent - 행사 등록 (관리자 전용)
     *
     * @param dto 신규 행사 정보
     * @return 생성된 행사 기본키
     */
    Long createEvent(LibraryEventDTO dto);

    /**
     * updateEvent - 행사 수정 (관리자 전용)
     *
     * @param id  수정할 행사 기본키
     * @param dto 수정할 행사 정보
     */
    void updateEvent(Long id, LibraryEventDTO dto);

    /**
     * deleteEvent - 행사 삭제 (관리자 전용)
     *
     * @param id 삭제할 행사 기본키
     */
    void deleteEvent(Long id);
}
