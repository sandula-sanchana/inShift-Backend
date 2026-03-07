package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredentialDescriptorDTO {
    private String type;
    private String id; // Base64URL credential ID
}