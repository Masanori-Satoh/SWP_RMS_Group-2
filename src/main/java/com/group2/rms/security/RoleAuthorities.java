package com.group2.rms.security;

/** Authorities for the roles defined by the RMS requirements. */
public final class RoleAuthorities {

    public static final String SYSTEM_ADMIN = "ROLE_SYSTEM_ADMIN";

    private RoleAuthorities() {
    }

    public static String fromRoleName(String roleName) {
        if (roleName == null) {
            throw new IllegalArgumentException("Role is missing");
        }

        return switch (roleName.trim()) {
            case "System Admin" -> SYSTEM_ADMIN;
            case "HR" -> "ROLE_HR";
            case "Hiring Manager" -> "ROLE_HIRING_MANAGER";
            case "Director" -> "ROLE_DIRECTOR";
            case "Interviewer" -> "ROLE_INTERVIEWER";
            case "Candidate" -> "ROLE_CANDIDATE";
            default -> throw new IllegalArgumentException("Unsupported role");
        };
    }
}
