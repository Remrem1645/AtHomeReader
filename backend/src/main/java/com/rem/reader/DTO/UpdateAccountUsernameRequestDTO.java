package com.rem.reader.DTO;

public class UpdateAccountUsernameRequestDTO {
    private String newUsername;

    public UpdateAccountUsernameRequestDTO(String newUsername) {
        this.newUsername = newUsername;
    }

    public String getNewUsername() { return newUsername; }
    public void setNewUsername(String newUsername) { this.newUsername = newUsername; }
}
