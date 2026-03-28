package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceCheckBiometricProofDTO {
    private String proofToken;
    private LocalDateTime expiresAt;
}