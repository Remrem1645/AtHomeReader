package com.rem.reader.DTO;

public class editBookRequestDTO {
    private String title;
    private String author;
    private String description;

    public editBookRequestDTO(String title, String author, String description) {
        this.title = title;
        this.author = author;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }
}
