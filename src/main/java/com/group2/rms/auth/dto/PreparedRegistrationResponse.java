package com.group2.rms.auth.dto;

// dto store account before store in db 
public record PreparedRegistrationResponse(
        String fullName,
        String username,
        String email,
        String passwordHash) {

    @Override
    public String toString() {
        return "PreparedRegistrationResponse";
    }
}
