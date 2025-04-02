package com.rem.reader.Repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rem.reader.Models.BookPageCache;

public interface BookPageCacheRepo extends JpaRepository<BookPageCache, Long> {

    List<BookPageCache> findByBookIdOrderByPageNumberAsc(UUID bookId);

    BookPageCache findByBookIdAndPageNumber(UUID bookId, int pageNumber);

    Page<BookPageCache> findByBookIdOrderByPageNumberAsc(UUID bookId, Pageable pageable);

    long countByBookId(UUID bookId);

    void deleteByBookId(UUID bookId);

    boolean existsByBookId(UUID bookId);
}
