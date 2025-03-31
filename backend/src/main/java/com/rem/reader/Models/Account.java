package com.rem.reader.Models;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Entity
@Table(name = "account")
public class Account {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private long id; 

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UUID")
    private UUID uuid; 

    @Column(name = "admin")
    private boolean admin;

    @Column(name = "username")
    private String username;
    
    @Column(name = "password")
    private String password;


    // Getters
    public long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; } 


    // Getters and Setters
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public UUID getUuid() { return uuid; }

    public void setAdmin(boolean admin) { this.admin = admin; }
    public boolean isAdmin() { return admin; }

    public void setUsername(String username) { this.username = username; }
    public String getUsername() { return username; } 


    // Password handling methods
    public void setPassword(String rawPassword) { 
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        this.password = encoder.encode(rawPassword);
    }

    public boolean checkPassword(String rawPassword) { 
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(rawPassword, this.password); 
    }
}
