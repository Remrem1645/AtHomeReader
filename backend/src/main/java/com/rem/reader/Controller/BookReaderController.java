package com.rem.reader.Controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rem.reader.Service.BookReaderService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/book-reader")
public class BookReaderController {

    @Autowired
    BookReaderService bookReaderService;

    @GetMapping("/{uuid}/{pages}")
    public ResponseEntity<?> getBookPages(@PathVariable UUID uuid, @PathVariable int pages, HttpSession session) {
        return bookReaderService.getBookPages(uuid, pages, session);
    }

    @GetMapping("/{uuid}/assets/{filename}")
    public ResponseEntity<?> getCurrentBookAsset(@PathVariable UUID uuid, @PathVariable String filename) {
        return bookReaderService.getCurrentBookAsset(uuid, filename);
    }
}
