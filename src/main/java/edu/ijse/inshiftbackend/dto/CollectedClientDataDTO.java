package edu.ijse.inshiftbackend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectedClientDataDTO {
    private String type;
    private String challenge;
    private String origin;
    private Boolean crossOrigin;
}