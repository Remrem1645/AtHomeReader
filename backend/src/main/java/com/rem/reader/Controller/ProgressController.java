package com.rem.reader.Controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rem.reader.Service.ProgressService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    @Autowired
    private ProgressService progressService;

    @DeleteMapping("/{bookUuid}")
    public ResponseEntity<?> removeUserProgress(@PathVariable UUID bookUuid, HttpSession session) {
        return progressService.removeUserProgress(bookUuid, session);
    }
}
