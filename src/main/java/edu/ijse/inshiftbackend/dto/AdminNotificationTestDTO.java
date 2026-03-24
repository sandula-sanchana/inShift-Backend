package edu.ijse.inshiftbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminNotificationTestDTO {

    @NotNull(message = "Employee id is required")
    private Long employeeId;

    private String title;
    private String body;
}