package edu.ijse.inshiftbackend.dto;

import edu.ijse.inshiftbackend.entity.enums.PresenceCheckResponseSource;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpPresenceCheckRespondDTO {

    @NotNull(message = "Presence check ID is required")
    private Long presenceCheckId;

    @NotNull(message = "Response source is required")
    private PresenceCheckResponseSource responseSource;

    private Double latitude;
    private Double longitude;
    private Double accuracyMeters;
    private String locationText;
    private String responseNote;
}