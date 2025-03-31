package com.rem.reader.Controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.rem.reader.DTO.EditBookRequestDTO;
import com.rem.reader.Service.BookService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadBook(@RequestParam("file") MultipartFile file) {
        return bookService.uploadBook(file);
    }

    @PutMapping("/{uuid}/edit")
    public ResponseEntity<?> editBook(@PathVariable UUID uuid, EditBookRequestDTO editBookRequest, HttpSession session) {
        return bookService.editBook(uuid, editBookRequest, session);
    }

    @DeleteMapping("/{uuid}/delete")
    public ResponseEntity<?> deleteBook(@PathVariable UUID uuid) {
        return bookService.deleteBook(uuid);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<?> getBook(@PathVariable UUID uuid, HttpSession session) {
        return bookService.getBook(uuid, session);
    }
    
    @GetMapping("/{uuid}/cover")
    public ResponseEntity<?> getBookCover(@PathVariable UUID uuid) {
        return bookService.getBookCover(uuid);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllBooks(HttpSession session) {
        return bookService.getAllBooks(session);
    }
}
