package com.group2.rms.auth.dto;
//dto response account
public record AccountSnapshotResponse(
        Integer userId,
        String email,
        String passwordHash) {

    @Override
    public String toString() {
        return "AccountSnapshotResponse[userId=" + userId + ", email=" + email + ", passwordHash=****]";
    }
}