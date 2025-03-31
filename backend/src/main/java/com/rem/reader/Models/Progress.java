package com.rem.reader.Models;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "progress")
public class Progress {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; 

    @Column(name = "account_uuid")
    private UUID accountUuid; 

    @Column(name = "book_uuid")
    private UUID bookUuid;
    
    @Column(name = "current_page_number")
    private int currentPageNumber;

    @Column(name = "favorite")
    private boolean favorite;

    
    // Getters
    public long getId() { return id; }

    
    // Getters and Setters
    public void setAccountUuid(UUID accountUuid) { this.accountUuid = accountUuid; }
    public UUID getAccountUuid() { return accountUuid; }

    public void setBookUuid(UUID bookUuid) { this.bookUuid = bookUuid; }
    public UUID getBookUuid() { return bookUuid; }

    public void setCurrentPageNumber(int currentPageNumber) { this.currentPageNumber = currentPageNumber; }
    public int getCurrentPageNumber() { return currentPageNumber; } 

    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public boolean isFavorite() { return favorite; }
}
