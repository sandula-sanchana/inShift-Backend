package edu.ijse.inshiftbackend.dto;

import edu.ijse.inshiftbackend.entity.enums.Role;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class EmployeeDTO {
    @Nullable
    private Long employeeId;

    @NotBlank(message = "Employee code is required")
    @Size(min = 2, max = 20, message = "Employee code must be 2-20 characters")
    private String empCode;

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 120, message = "Full name must be 3-120 characters")
    private String fullName;

    @Nullable
    @Email(message = "Invalid email format")
    @Size(max = 120, message = "Email cannot exceed 120 characters")
    private String email;

    @Nullable
    @Pattern(regexp = "^(\\+94|0)?7\\d{8}$", message = "Invalid Sri Lankan phone number")
    private String phone;

    @NotNull(message = "Role is required")
    @Pattern(
            regexp = "^(EMPLOYEE|SUPERVISOR|HR|ADMIN)$",
            message = "Role must be EMPLOYEE, SUPERVISOR, HR, or ADMIN"
    )
    private Role role;

    @NotNull(message = "Branch is required")
    private Long branchId;

    @Nullable
    private String branchName;

    @Nullable
    private String branchCode;

    @Nullable
    private Boolean mustChangePassword;

    @NotNull(message = "Active status must be specified")
    private Boolean active;

    @Nullable
    @Size(min = 6, max = 64, message = "Password must be 6-64 characters")
    private String password;
}
