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

    public Progress getUserProgress(UUID userUuid, UUID bookUuid) {
        return progressRepo.findByAccountUuidAndBookUuid(userUuid, bookUuid);
    }
}
