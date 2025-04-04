package com.rem.reader.Service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.rem.reader.Models.BookPageCache;
import com.rem.reader.Repo.BookPageCacheRepo;

@Service
public class BookPageCacheService {

    @Autowired
    BookPageCacheRepo bookPageCacheRepo;

    // Internal methods

    /**
     * Check if a book page cache entry exists for a given book ID.
     * @param bookId The UUID of the book.
     * @return true if the entry exists, otherwise false.
     */
    public boolean existsByBookId(UUID bookId) {
        return bookPageCacheRepo.existsByBookId(bookId);
    }

    /**
     * Get a paginated list of book pages for a given book ID, ordered by page number.
     * @param bookId The UUID of the book.
     * @param pageNumber The page number of the book.
     * @param pageSize The size of the page.
     * @return A paginated list of BookPageCache entities.
     */
    public Page<BookPageCache> getBookPageCacheByBookId(UUID bookId, int pageNumber, int pageSize) {
        return bookPageCacheRepo.findByBookIdOrderByPageNumberAsc(
                bookId,
                PageRequest.of(pageNumber, pageSize));
    }

    /**
     * Get all book page cache entries by book ID, ordered by page number in ascending order.
     * @param bookId The UUID of the book.
     * @return A list of BookPageCache entities.
     */
    public void deleteBookPageCacheByBookId(UUID bookId) {
        bookPageCacheRepo.deleteByBookId(bookId);
    }

    /**
     * Save a list of book page cache entries.
     * @param bookPageCache The list of BookPageCache entities to save.
     */
    public void saveBookPageCache(List<BookPageCache> bookPageCache) {
        bookPageCacheRepo.saveAll(bookPageCache);
    }
}
