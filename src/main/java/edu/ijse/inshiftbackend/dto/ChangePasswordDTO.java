package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDTO {
    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 6, max = 64)
    private String newPassword;
}