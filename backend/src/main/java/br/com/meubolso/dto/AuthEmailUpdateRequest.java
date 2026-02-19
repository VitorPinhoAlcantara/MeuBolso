package br.com.meubolso.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthEmailUpdateRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String newEmail;

    @NotBlank
    @Size(min = 6, max = 255)
    private String currentPassword;

    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
}
