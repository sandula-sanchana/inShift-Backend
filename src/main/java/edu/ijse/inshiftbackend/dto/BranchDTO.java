package edu.ijse.inshiftbackend.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDTO {

    @Nullable
    private Long branchId;

    @NotBlank(message = "Branch code is required")
    @Size(min = 2, max = 20, message = "Branch code must be between 2 and 20 characters")
    private String branchCode;

    @NotBlank(message = "Branch name is required")
    @Size(min = 3, max = 100, message = "Branch name must be between 3 and 100 characters")
    private String branchName;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 150, message = "Address line 1 cannot exceed 150 characters")
    private String addressLine1;

    @Size(max = 150, message = "Address line 2 cannot exceed 150 characters")
    @Nullable
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 50)
    private String city;

    @NotBlank(message = "District is required")
    @Size(max = 50)
    private String district;

    @NotBlank(message = "Province is required")
    @Size(max = 50)
    private String province;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotBlank(message = "Contact number is required")
    @Pattern(
            regexp = "^(\\+94|0)?7\\d{8}$",
            message = "Invalid Sri Lankan contact number"
    )
    private String contactNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Active status must be specified")
    private Boolean active;
}
