package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceFlagDTO {
    private Long id;
    private String flagType;
    private String severity;
    private Integer scoreImpact;
    private String message;
    private Boolean resolved;
}