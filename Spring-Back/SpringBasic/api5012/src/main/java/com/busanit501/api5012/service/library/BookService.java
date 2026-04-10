package com.busanit501.api5012.service.library;

import com.busanit501.api5012.dto.library.BookDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * BookService - 도서 서비스 인터페이스
 *
 * 부산도서관 관리 시스템의 도서 관련 비즈니스 로직을 정의합니다.
 * 도서 검색, 대여 가능 목록 조회, 관리자용 등록/수정/삭제 기능을 제공합니다.
 *
 * [페이지네이션(Pagination) 설명]
 * Spring Data JPA 의 Pageable 인터페이스를 사용하여 페이지 번호, 크기, 정렬 기준을 전달합니다.
 * Page<T> 타입으로 반환하면 총 건수, 총 페이지 수 등의 메타 정보도 함께 제공됩니다.
 */
public interface BookService {

    /**
     * getBooks - 도서 목록 조회 (키워드 검색 + 페이지네이션)
     *
     * keyword 가 비어 있으면 전체 도서를 조회합니다.
     * keyword 가 있으면 도서명, 저자명, 출판사명 통합 검색을 수행합니다.
     *
     * @param keyword  검색 키워드 (null 또는 빈 문자열이면 전체 조회)
     * @param pageable 페이지 정보 (번호, 크기, 정렬)
     * @return 페이지네이션이 적용된 도서 목록
     */
    Page<BookDTO> getBooks(String keyword, Pageable pageable);

    /**
     * getBookById - 도서 상세 조회
     *
     * @param id 조회할 도서 기본키
     * @return 도서 상세 정보 DTO
     * @throws RuntimeException 해당 ID의 도서가 없을 때
     */
    BookDTO getBookById(Long id);

    /**
     * registerBook - 도서 등록 (관리자 전용)
     *
     * 새 도서를 시스템에 등록합니다.
     * ISBN 중복 검사를 수행합니다.
     *
     * @param dto 등록할 도서 정보
     * @return 등록된 도서의 ID (기본키)
     * @throws IllegalArgumentException ISBN 중복 시
     */
    Long registerBook(BookDTO dto);

    /**
     * updateBook - 도서 정보 수정 (관리자 전용)
     *
     * 도서명, 저자, 출판사, 설명을 수정합니다.
     *
     * @param id  수정할 도서 기본키
     * @param dto 수정할 도서 정보
     * @throws RuntimeException 해당 ID의 도서가 없을 때
     */
    void updateBook(Long id, BookDTO dto);

    /**
     * changeBookStatus - 도서 상태 직접 변경 (관리자용)
     * 분실(LOST), 대여가능(AVAILABLE) 등으로 상태를 수동 변경합니다.
     *
     * @param id     변경할 도서 ID
     * @param status 변경할 상태 문자열 (BookStatus enum 이름)
     */
    void changeBookStatus(Long id, String status);

    /**
     * deleteBook - 도서 삭제 (관리자 전용)
     *
     * 도서를 시스템에서 완전히 삭제합니다.
     * 대여 중인 도서는 삭제 불가 처리를 권장합니다.
     *
     * @param id 삭제할 도서 기본키
     * @throws RuntimeException 해당 ID의 도서가 없을 때
     */
    void deleteBook(Long id);

    /**
     * getAvailableBooks - 대여 가능 도서 목록 조회
     *
     * BookStatus.AVAILABLE 상태의 도서만 반환합니다.
     * 대여 신청 화면에서 사용합니다.
     *
     * @param pageable 페이지 정보
     * @return 대여 가능한 도서 목록
     */
    Page<BookDTO> getAvailableBooks(Pageable pageable);
}
