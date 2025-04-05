package com.rem.reader.Models;

import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(
    name = "book_page_cache",
    indexes = {
        @Index(name = "idx_book_id", columnList = "book_uuid"),
        @Index(name = "idx_book_page", columnList = "book_uuid, page_number")
    }
)
public class BookPageCache {
    
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_uuid")
    private UUID bookId;

    @Column(name = "title")
    private String title;

    @Column(name = "page_number")
    private int pageNumber;

    @Column(name = "content")
    private String content;

    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getBookId() { return bookId; }
    public void setBookId(UUID bookId) { this.bookId = bookId; }

    public String getTitle() { return title; }
    public void setTitle(String title) {this.title = title; }

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
