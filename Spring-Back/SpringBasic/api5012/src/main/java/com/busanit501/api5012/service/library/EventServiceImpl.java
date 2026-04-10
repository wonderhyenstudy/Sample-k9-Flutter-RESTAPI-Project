package com.busanit501.api5012.service.library;

import com.busanit501.api5012.domain.library.EventApplication;
import com.busanit501.api5012.domain.library.LibraryEvent;
import com.busanit501.api5012.domain.library.Member;
import com.busanit501.api5012.dto.library.EventApplicationDTO;
import com.busanit501.api5012.dto.library.LibraryEventDTO;
import com.busanit501.api5012.repository.library.EventApplicationRepository;
import com.busanit501.api5012.repository.library.LibraryEventRepository;
import com.busanit501.api5012.repository.library.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EventServiceImpl - 도서관 행사 서비스 구현체
 *
 * EventService 인터페이스의 구현 클래스입니다.
 * 행사 목록/상세 조회, 행사 신청 및 취소 기능을 구현합니다.
 *
 * [행사 신청 시 동시성 고려]
 * 정원 마감 처리(increaseParticipants)는 LibraryEvent 엔티티의 도메인 메서드를 활용합니다.
 * 실제 서비스 환경에서는 낙관적 락(Optimistic Lock) 또는 비관적 락(Pessimistic Lock)을
 * 추가하여 동시 신청 처리를 안전하게 관리하는 것을 권장합니다.
 * (교육 목적으로 간단한 구현만 포함합니다.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    /** LibraryEventRepository - 행사 엔티티에 대한 DB 접근 */
    private final LibraryEventRepository libraryEventRepository;

    /** EventApplicationRepository - 행사 신청 엔티티에 대한 DB 접근 */
    private final EventApplicationRepository eventApplicationRepository;

    /** MemberRepository - 회원 엔티티에 대한 DB 접근 */
    private final MemberRepository memberRepository;

    /**
     * getEvents - 행사 목록 조회 (페이지네이션)
     *
     * 전체 행사를 행사일 오름차순으로 조회합니다.
     * Page.map() 으로 LibraryEvent → LibraryEventDTO 변환합니다.
     */
    @Override
    public Page<LibraryEventDTO> getEvents(Pageable pageable) {
        log.info("행사 목록 조회 - page: {}", pageable.getPageNumber());

        // 행사일 오름차순 전체 조회
        Page<LibraryEvent> eventPage = libraryEventRepository.findAllByOrderByEventDateAsc(pageable);
        return eventPage.map(LibraryEventDTO::fromEntity);
    }

    /**
     * getEventsByDateRange - 기간별 행사 조회
     *
     * 시작일 ~ 종료일 사이에 개최되는 행사를 조회합니다.
     * 월별 행사 캘린더 화면에서 사용합니다.
     */
    @Override
    public List<LibraryEventDTO> getEventsByDateRange(LocalDate start, LocalDate end) {
        log.info("기간별 행사 조회 - start: {}, end: {}", start, end);

        List<LibraryEvent> events = libraryEventRepository.findByEventDateBetween(start, end);

        // Stream API: List<LibraryEvent> → List<LibraryEventDTO> 변환
        return events.stream()
                .map(LibraryEventDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * getEventById - 행사 상세 조회
     *
     * 잔여 신청 가능 인원(remainingSlots)이 포함된 상세 정보를 반환합니다.
     */
    @Override
    public LibraryEventDTO getEventById(Long id) {
        log.info("행사 상세 조회 - eventId: {}", id);

        LibraryEvent event = libraryEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("행사를 찾을 수 없습니다. id: " + id));

        return LibraryEventDTO.fromEntity(event);
    }

    /**
     * applyEvent - 행사 신청
     *
     * [처리 순서]
     * 1. 행사 존재 확인
     * 2. 회원 존재 확인
     * 3. 신청 가능 상태(isOpen) 확인
     * 4. 중복 신청 확인
     * 5. EventApplication 생성
     * 6. LibraryEvent.increaseParticipants() 호출 (참가 인원 +1, 자동 CLOSED 처리)
     */
    @Override
    @Transactional
    public Long applyEvent(Long eventId, Long memberId) {
        log.info("행사 신청 시작 - eventId: {}, memberId: {}", eventId, memberId);

        // 1단계: 행사 조회
        LibraryEvent event = libraryEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("행사를 찾을 수 없습니다. id: " + eventId));

        // 2단계: 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다. id: " + memberId));

        // 3단계: 신청 가능 상태 확인 (OPEN + 정원 미달)
        if (!event.isOpen()) {
            throw new IllegalStateException(
                    "신청이 마감된 행사입니다. 현재 상태: " + event.getStatus()
                    + ", 현재 인원: " + event.getCurrentParticipants()
                    + "/" + event.getMaxParticipants());
        }

        // 4단계: 중복 신청 확인
        boolean alreadyApplied = eventApplicationRepository.existsByEventIdAndMemberId(eventId, memberId);
        if (alreadyApplied) {
            throw new IllegalStateException("이미 신청한 행사입니다. eventId: " + eventId);
        }

        // 5단계: EventApplication 엔티티 생성
        EventApplication application = EventApplication.builder()
                .event(event)
                .member(member)
                .build(); // status 기본값: "APPLIED", applyDate 기본값: now()

        Long applicationId = eventApplicationRepository.save(application).getId();

        // 6단계: 행사 참가 인원 증가 (정원 도달 시 CLOSED 자동 처리)
        event.increaseParticipants();
        // 더티체킹으로 LibraryEvent 자동 UPDATE

        log.info("행사 신청 완료 - applicationId: {}, eventId: {}, memberId: {}",
                applicationId, eventId, memberId);

        return applicationId;
    }

    /**
     * cancelEventApplication - 행사 신청 취소
     *
     * 신청 상태를 CANCELLED 로 변경하고, 행사 참가 인원을 감소시킵니다.
     * 인원 감소 후 CLOSED → OPEN 복원이 가능합니다.
     */
    @Override
    @Transactional
    public void cancelEventApplication(Long applicationId) {
        log.info("행사 신청 취소 시작 - applicationId: {}", applicationId);

        // 신청 기록 조회
        EventApplication application = eventApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException(
                        "행사 신청 기록을 찾을 수 없습니다. id: " + applicationId));

        // 이미 취소된 신청인지 확인
        if (!application.isActive()) {
            throw new IllegalStateException("이미 취소된 신청입니다. applicationId: " + applicationId);
        }

        // 신청 상태를 CANCELLED 로 변경 (도메인 메서드 활용)
        application.cancel();

        // 행사 참가 인원 감소 (CLOSED → OPEN 자동 복원 가능)
        application.getEvent().decreaseParticipants();

        log.info("행사 신청 취소 완료 - applicationId: {}", applicationId);
    }

    /**
     * getMyEventApplications - 내 행사 신청 목록 조회
     *
     * JOIN FETCH 쿼리로 행사 정보와 회원 정보를 한 번에 로딩합니다.
     */
    @Override
    public List<EventApplicationDTO> getMyEventApplications(Long memberId) {
        log.info("내 행사 신청 목록 조회 - memberId: {}", memberId);

        // JOIN FETCH 로 event + member 정보 함께 로딩 (N+1 방지)
        List<EventApplication> applications =
                eventApplicationRepository.findMemberApplicationsWithEventDetails(memberId);

        return applications.stream()
                .map(EventApplicationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * createEvent - 행사 등록 (관리자 전용)
     * DTO 의 필수 필드를 바탕으로 LibraryEvent 엔티티를 생성합니다.
     */
    @Override
    @Transactional
    public Long createEvent(LibraryEventDTO dto) {
        log.info("행사 등록 - title: {}, eventDate: {}", dto.getTitle(), dto.getEventDate());

        LibraryEvent event = LibraryEvent.builder()
                .category(dto.getCategory())
                .title(dto.getTitle())
                .content(dto.getContent())
                .eventDate(dto.getEventDate())
                .place(dto.getPlace())
                .maxParticipants(dto.getMaxParticipants())
                .currentParticipants(0)
                .status("OPEN")
                .build();

        Long savedId = libraryEventRepository.save(event).getId();
        log.info("행사 등록 완료 - eventId: {}", savedId);
        return savedId;
    }

    /**
     * updateEvent - 행사 수정 (관리자 전용)
     * 도메인 updateInfo() 호출 후 더티체킹으로 자동 UPDATE.
     */
    @Override
    @Transactional
    public void updateEvent(Long id, LibraryEventDTO dto) {
        log.info("행사 수정 - eventId: {}", id);

        LibraryEvent event = libraryEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("행사를 찾을 수 없습니다. id: " + id));

        event.updateInfo(
                dto.getCategory(),
                dto.getTitle(),
                dto.getContent(),
                dto.getEventDate(),
                dto.getPlace(),
                dto.getMaxParticipants()
        );

        // 상태 변경이 요청된 경우만 반영
        if (dto.getStatus() != null) {
            if ("OPEN".equalsIgnoreCase(dto.getStatus())) {
                event.reopenEvent();
            } else if ("CLOSED".equalsIgnoreCase(dto.getStatus())) {
                event.closeEvent();
            }
        }

        log.info("행사 수정 완료 - eventId: {}", id);
    }

    /**
     * deleteEvent - 행사 삭제 (관리자 전용)
     * 관련 EventApplication 은 FK 제약에 따라 사전 삭제가 필요할 수 있습니다.
     */
    @Override
    @Transactional
    public void deleteEvent(Long id) {
        log.info("행사 삭제 - eventId: {}", id);

        LibraryEvent event = libraryEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("행사를 찾을 수 없습니다. id: " + id));

        libraryEventRepository.delete(event);
        log.info("행사 삭제 완료 - eventId: {}", id);
    }
}
