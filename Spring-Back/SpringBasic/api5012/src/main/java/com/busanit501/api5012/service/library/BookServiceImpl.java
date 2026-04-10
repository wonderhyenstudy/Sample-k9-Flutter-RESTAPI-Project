package com.busanit501.api5012.service.library;

import com.busanit501.api5012.domain.library.Book;
import com.busanit501.api5012.domain.library.BookStatus;
import com.busanit501.api5012.domain.library.RentalStatus;
import com.busanit501.api5012.dto.library.BookDTO;
import com.busanit501.api5012.repository.library.BookRepository;
import com.busanit501.api5012.repository.library.RentalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * BookServiceImpl - 도서 서비스 구현체
 *
 * BookService 인터페이스의 구현 클래스입니다.
 * 도서 검색, 상세 조회, 등록/수정/삭제, 대여 가능 도서 조회 기능을 구현합니다.
 *
 * [Page<T> 와 Stream API 설명]
 * Page<Book> 은 도서 엔티티 목록과 페이징 메타 정보를 함께 담는 래퍼 객체입니다.
 * .map() 메서드로 Page<Book> 을 Page<BookDTO> 로 변환합니다.
 * Stream API 의 map() 과 달리 Page 의 메타 정보(총 건수, 페이지 수)는 그대로 유지됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본: 읽기 전용
public class BookServiceImpl implements BookService {

    /** BookRepository - 도서 엔티티에 대한 DB 접근 */
    private final BookRepository bookRepository;

    /** RentalRepository - 도서 상태 변경 시 대여 기록 동기화에 사용 */
    private final RentalRepository rentalRepository;

    /**
     * getBooks - 도서 목록 조회 (키워드 검색 + 페이지네이션)
     *
     * keyword 유무에 따라 전체 조회 또는 키워드 검색을 분기 처리합니다.
     *
     * [Page.map() 사용 예시]
     * Page<Book> bookPage = bookRepository.searchByKeyword(keyword, pageable);
     * bookPage.map(BookDTO::fromEntity) 는 각 Book 엔티티를 BookDTO 로 변환합니다.
     * BookDTO::fromEntity 는 메서드 참조(Method Reference) 표현입니다.
     *   = book -> BookDTO.fromEntity(book) 와 동일합니다.
     */
    @Override
    public Page<BookDTO> getBooks(String keyword, Pageable pageable) {
        log.info("도서 목록 조회 - keyword: {}, page: {}", keyword, pageable.getPageNumber());

        // keyword 가 null 이거나 빈 문자열이면 전체 도서 조회
        if (keyword == null || keyword.isBlank()) {
            // 최신 등록순 전체 조회
            Page<Book> bookPage = bookRepository.findAllByOrderByRegDateDesc(pageable);
            // Page<Book> → Page<BookDTO> 변환 (메서드 참조 활용)
            return bookPage.map(BookDTO::fromEntity);
        }

        // keyword 가 있으면 도서명 + 저자명 + 출판사명 통합 검색
        Page<Book> bookPage = bookRepository.searchByKeyword(keyword, pageable);
        return bookPage.map(BookDTO::fromEntity);
    }

    /**
     * getBookById - 도서 상세 조회
     *
     * Optional.orElseThrow() 로 도서가 없으면 예외를 발생시킵니다.
     */
    @Override
    public BookDTO getBookById(Long id) {
        log.info("도서 상세 조회 - bookId: {}", id);

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 도서를 찾을 수 없습니다. id: " + id));

        return BookDTO.fromEntity(book);
    }

    /**
     * registerBook - 도서 등록 (관리자 전용)
     *
     * ISBN 중복 검사 후 도서를 등록합니다.
     * BookDTO.toEntity() 메서드를 활용하여 엔티티를 생성합니다.
     */
    @Override
    @Transactional // 쓰기 트랜잭션 활성화
    public Long registerBook(BookDTO dto) {
        log.info("도서 등록 시작 - 제목: {}, ISBN: {}", dto.getBookTitle(), dto.getIsbn());

        // ISBN 중복 확인 (ISBN 이 있는 경우만 검사)
        if (dto.getIsbn() != null && !dto.getIsbn().isBlank()) {
            if (bookRepository.existsByIsbn(dto.getIsbn())) {
                throw new IllegalArgumentException("이미 등록된 ISBN입니다: " + dto.getIsbn());
            }
        }

        // BookDTO.toEntity() 로 엔티티 생성 (상태는 기본값 AVAILABLE)
        Book book = dto.toEntity();
        Long savedId = bookRepository.save(book).getId();

        log.info("도서 등록 완료 - bookId: {}, 제목: {}", savedId, dto.getBookTitle());
        return savedId;
    }

    /**
     * updateBook - 도서 정보 수정 (관리자 전용)
     *
     * Book 엔티티의 updateInfo() 도메인 메서드를 호출하여 정보를 수정합니다.
     * @Transactional + 더티체킹으로 자동 UPDATE 됩니다.
     */
    @Override
    @Transactional
    public void updateBook(Long id, BookDTO dto) {
        log.info("도서 수정 시작 - bookId: {}, dto.status: {}", id, dto.getStatus());

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 도서를 찾을 수 없습니다. id: " + id));

        log.info("도서 수정 - 현재 DB status: {}", book.getStatus());

        // 도메인 비즈니스 메서드로 필드 수정 (더티체킹으로 자동 UPDATE)
        book.updateInfo(
                dto.getBookTitle(),
                dto.getAuthor(),
                dto.getPublisher(),
                dto.getDescription()
        );

        // status 필드가 전달된 경우 상태도 변경 (관리자 드롭다운 반영)
        if (dto.getStatus() != null && dto.getStatus() != book.getStatus()) {
            BookStatus newStatus = dto.getStatus();
            log.info("도서 상태 변경 감지 - {} → {}", book.getStatus(), newStatus);
            book.changeStatus(newStatus);
            // AVAILABLE 로 변경 시 활성 대여 기록도 RETURNED 로 동기화
            if (newStatus == BookStatus.AVAILABLE) {
                rentalRepository.findActiveRentalByBookId(id).ifPresent(rental -> {
                    rental.processReturn(LocalDate.now());
                    log.info("대여 기록 자동 반납 처리 - rentalId: {}", rental.getId());
                });
            }
        } else {
            log.info("도서 상태 변경 없음 - dto.status: {}, book.status: {}",
                    dto.getStatus(), book.getStatus());
        }

        log.info("도서 수정 완료 - bookId: {}, 최종 status: {}", id, book.getStatus());
    }

    /**
     * deleteBook - 도서 삭제 (관리자 전용)
     *
     * 대여 중인 도서는 삭제하지 않도록 상태 확인 후 삭제합니다.
     */
    @Override
    @Transactional
    public void deleteBook(Long id) {
        log.info("도서 삭제 시작 - bookId: {}", id);

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 도서를 찾을 수 없습니다. id: " + id));

        // 대여 중인 도서는 삭제 불가 처리
        if (book.getStatus() == BookStatus.RENTED) {
            throw new IllegalStateException("현재 대여 중인 도서는 삭제할 수 없습니다. id: " + id);
        }

        bookRepository.delete(book);
        log.info("도서 삭제 완료 - bookId: {}", id);
    }

    /**
     * changeBookStatus - 도서 상태 직접 변경 (관리자용)
     * 분실(LOST), 강제 복원(AVAILABLE) 등 수동 상태 조정에 사용합니다.
     */
    @Override
    @Transactional
    public void changeBookStatus(Long id, String status) {
        log.info("도서 상태 변경 요청 - bookId: {}, status: {}", id, status);

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 도서를 찾을 수 없습니다. id: " + id));

        BookStatus bookStatus = BookStatus.valueOf(status.toUpperCase());
        book.changeStatus(bookStatus);

        // AVAILABLE 로 변경 시 활성 대여 기록도 RETURNED 로 동기화
        if (bookStatus == BookStatus.AVAILABLE) {
            rentalRepository.findActiveRentalByBookId(id).ifPresent(rental -> {
                rental.processReturn(LocalDate.now());
                log.info("대여 기록 자동 반납 처리 - rentalId: {}", rental.getId());
            });
        }

        log.info("도서 상태 변경 완료 - bookId: {}, newStatus: {}", id, bookStatus);
    }

    /**
     * getAvailableBooks - 대여 가능 도서 목록 조회
     *
     * BookStatus.AVAILABLE 상태의 도서만 반환합니다.
     * BookRepository.findAvailableBooks() 는 최신 등록순 정렬이 포함된 JPQL 쿼리입니다.
     */
    @Override
    public Page<BookDTO> getAvailableBooks(Pageable pageable) {
        log.info("대여 가능 도서 목록 조회 - page: {}", pageable.getPageNumber());

        Page<Book> bookPage = bookRepository.findAvailableBooks(pageable);
        return bookPage.map(BookDTO::fromEntity);
    }
}
