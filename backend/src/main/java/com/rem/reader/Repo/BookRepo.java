package com.rem.reader.Repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.rem.reader.Models.Book;

public interface BookRepo extends JpaRepository<Book, Long> {

    /**
     * Find a book by its UUID.
     * @param uuid The UUID of the book to find.
     * @return The Book entity if found, otherwise null.
     */
    @Query(value = "SELECT * FROM books WHERE UUID = ?1", nativeQuery = true)
    Book findByUuid(UUID uuid); 

}
