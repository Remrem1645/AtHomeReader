package com.rem.reader.Repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.rem.reader.Models.Progress;

import jakarta.transaction.Transactional;

public interface ProgressRepo extends JpaRepository<Progress, Long> {

    /**
     * Check if a progress entry exists for the given user and book UUIDs.
     * @param userUuid The UUID of the user.
     * @param bookUuid The UUID of the book.
     * @return true if a progress entry exists, otherwise false.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Progress p WHERE p.accountUuid = ?1 AND p.bookUuid = ?2")
    boolean existsByAccountUuidAndBookUuid(UUID userUuid, UUID bookUuid); 


    /**
     * Update the current page number for a given user's progress on a specific book.
     * @param userUuid The UUID of the user whose progress is to be updated.
     * @param bookUuid The UUID of the book for which the progress is to be updated.
     * @param currentPage  The new current page number to set for the user's progress on the book.
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE progress SET current_page_number = ?3 WHERE account_uuid = ?1 AND book_uuid = ?2", nativeQuery = true)
    void updateCurrentPage(UUID userUuid, UUID bookUuid, int currentPage);


    /**
     * Find the progress entry for a specific user and book UUIDs.
     * @param userUuid The UUID of the user.
     * @param bookUuid The UUID of the book.
     * @return The Progress entity if found, otherwise null.
     */
    @Query("SELECT p FROM Progress p WHERE p.accountUuid = ?1 AND p.bookUuid = ?2")
    Progress findByAccountUuidAndBookUuid(UUID userUuid, UUID bookUuid);

} 
