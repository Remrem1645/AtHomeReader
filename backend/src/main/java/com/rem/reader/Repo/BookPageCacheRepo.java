package com.rem.reader.Repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.rem.reader.Models.BookPageCache;

import jakarta.transaction.Transactional;

public interface BookPageCacheRepo extends JpaRepository<BookPageCache, Long> {

    /**
     * Find all book page cache entries by book ID, ordered by page number in ascending order.
     * @param bookId The UUID of the book.
     * @return A list of BookPageCache entities.
     */
    List<BookPageCache> findByBookIdOrderByPageNumberAsc(UUID bookId);

    /**
     * Find a book page cache entry by book ID and page number.
     * @param bookId The UUID of the book.
     * @param pageNumber The page number of the book.
     */
    BookPageCache findByBookIdAndPageNumber(UUID bookId, int pageNumber);

    /**
     * Find a paginated list of book pages for a given book ID, ordered by page number.
     * @param bookId The UUID of the book.
     * @param pageable The pagination information.
     * @return A paginated list of BookPageCache entities.
     */
    Page<BookPageCache> findByBookIdOrderByPageNumberAsc(UUID bookId, Pageable pageable);

    /**
     * Count the number of pages for a given book ID.
     * @param bookId The UUID of the book.
     * @return The number of pages for the book.
     */
    long countByBookId(UUID bookId);

    /**
     * Check if a book page cache entry exists for a given book ID.
     * @param bookId The UUID of the book.
     * @return true if the entry exists, otherwise false.
     */
    boolean existsByBookId(UUID bookId);

    /**
     * Delete all pages for a given book ID.
     * @param bookId The UUID of the book.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM book_page_cache WHERE book_uuid = ?1", nativeQuery = true)
    void deleteByBookUuid(UUID bookId);
}
