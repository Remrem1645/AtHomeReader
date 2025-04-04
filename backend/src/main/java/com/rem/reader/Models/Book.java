package com.rem.reader.Models;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "book") 
public class Book {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "UUID") 
    private UUID uuid;

    @Column(name = "title")
    private String title = ""; 

    @Column(name = "author")
    private String author = "";

    @Column(name = "pages")
    private int pages;

    @Column(name = "description")
    private String description = "";

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "cover_image_path")
    private String coverImagePath;


    // Getters
    public long getId() { return id; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }


    // Getters and Setters 
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public UUID getUuid() { return uuid; }

    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }

    public void setAuthor(String author) { this.author = author; }
    public String getAuthor() { return author; }

    public void setPages(int pages) { this.pages = pages; }
    public int getPages() { return pages; }

    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFilePath() { return filePath; }

    public void setDescription(String description) { this.description = description; }
    public String getDescription() { return description; }

    public void setCoverImagePath(String coverImagePath) { this.coverImagePath = coverImagePath; }
    public String getCoverImagePath() { return coverImagePath; }
}
