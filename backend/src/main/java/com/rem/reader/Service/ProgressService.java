package com.rem.reader.Service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.rem.reader.Models.Progress;
import com.rem.reader.Repo.ProgressRepo;

import jakarta.servlet.http.HttpSession;

@Service
public class ProgressService {

    @Autowired
    ProgressRepo progressRepo;

    // Public methods

    /**
     * Removes the user's progress for a specific book.
     * 
     * @param bookUuid The UUID of the book.
     * @param session  The HTTP session.
     * @return A ResponseEntity indicating the result of the operation.
     */
    public ResponseEntity<?> removeUserProgress(UUID bookUuid, HttpSession session) {
        UUID userUuid = (UUID) session.getAttribute("userUuid");
        if (userUuid == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Progress progress = progressRepo.findByAccountUuidAndBookUuid(userUuid, bookUuid);
        if (progress != null) {
            progressRepo.delete(progress);
            return ResponseEntity.ok("Progress removed successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Internal methods

    /**
     * Updates the user's current page for a specific book. If the progress entry
     * does not exist, it creates a new one.
     * 
     * @param userUuid    The UUID of the user.
     * @param bookUuid    The UUID of the book.
     * @param currentPage The current page number.
     */
    public void updateUserCurrentPage(UUID userUuid, UUID bookUuid, int currentPage) {
        if (progressRepo.existsByAccountUuidAndBookUuid(userUuid, bookUuid)) {
            progressRepo.updateCurrentPage(userUuid, bookUuid, currentPage);
        } 
        else {
            Progress progress = new Progress();
            progress.setAccountUuid(userUuid); 
            progress.setBookUuid(bookUuid); 
            progress.setCurrentPageNumber(currentPage);
            progress.setFavorite(false);

            progressRepo.save(progress); 
        }
    }

    /**
     * Retrieves the user's progress for a specific book.
     * 
     * @param userUuid The UUID of the user.
     * @param bookUuid The UUID of the book.
     * @return The user's progress for the specified book.
     */
    public Progress getUserProgress(UUID userUuid, UUID bookUuid) {
        return progressRepo.findByAccountUuidAndBookUuid(userUuid, bookUuid);
    }

    /**
     * Removes all progress entries for a specific book.
     * 
     * @param bookUuid The UUID of the book.
     */
    public void removeAllProggressByBookId(UUID bookUuid) {
        progressRepo.deleteByBookUuid(bookUuid);
    }
}
