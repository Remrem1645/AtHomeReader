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
}
