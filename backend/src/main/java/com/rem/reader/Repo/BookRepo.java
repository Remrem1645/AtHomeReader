package com.rem.reader.Repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.rem.reader.Models.Book;

import jakarta.transaction.Transactional;

public interface BookRepo extends JpaRepository<Book, Long> {

    /**
     * Find a book by its UUID.
     * 
     * @param uuid The UUID of the book to find.
     * @return The Book entity if found, otherwise null.
     */
    Book findByUuid(UUID uuid);

    /**
     * update book pages by UUID
     * 
     * @param uuid  The UUID of the book to update.
     * @param pages The new number of pages.
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE book SET pages = ?2 WHERE uuid = ?1", nativeQuery = true)
    void updatePagesByUuid(UUID uuid, int pages);
}
