package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendancePunchDTO {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Type is required")
    @Pattern(regexp = "IN|OUT", message = "Type must be IN or OUT")
    private String type;

    private Double lat;
    private Double lng;

    @Size(max = 255)
    private String locationText;

    @Size(max = 500)
    private String reason;
}