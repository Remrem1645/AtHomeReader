package com.rem.reader.DTO;

public class AuthAccountDTO {
    
    private String username;
    private String password;

    public AuthAccountDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) {this.password = password; }
}
