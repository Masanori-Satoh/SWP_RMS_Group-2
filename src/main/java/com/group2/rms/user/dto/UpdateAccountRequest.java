package com.group2.rms.user.dto;

import com.group2.rms.user.service.AccountManagementService.UpdateCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAccountRequest {

    @NotBlank(message = "Enter a full name.")
    @Size(max = 100, message = "Full name must be at most 100 characters.")
    private String fullName;

    @NotBlank(message = "Enter an email address.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 150, message = "Email must be at most 150 characters.")
    private String email;

    @Size(max = 20, message = "Phone number must be at most 20 characters.")
    private String phoneNumber;

    @NotNull(message = "Select a role.")
    private Integer roleId;

    private Integer departmentId;

    @NotBlank(message = "Select a status.")
    private String accountStatus;

    public UpdateCommand toCommand() {
        return new UpdateCommand(fullName, email, phoneNumber, roleId, departmentId, accountStatus);
    }
}
