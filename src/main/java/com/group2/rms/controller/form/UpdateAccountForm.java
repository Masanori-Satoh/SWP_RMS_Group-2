package com.group2.rms.controller.form;

import com.group2.rms.service.AccountManagementService.UpdateCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAccountForm {

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không đúng định dạng.")
    @Size(max = 150, message = "Email tối đa 150 ký tự.")
    private String email;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự.")
    private String phoneNumber;

    @NotNull(message = "Vui lòng chọn vai trò.")
    private Integer roleId;

    private Integer departmentId;

    @NotBlank(message = "Vui lòng chọn trạng thái.")
    private String accountStatus;

    public UpdateCommand toCommand() {
        return new UpdateCommand(fullName, email, phoneNumber, roleId, departmentId, accountStatus);
    }
}
