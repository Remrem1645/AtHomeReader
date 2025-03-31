package com.rem.reader.DTO;

public class UpdateAccountPasswordDTO {
    private String newPassword;

    public UpdateAccountPasswordDTO(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
