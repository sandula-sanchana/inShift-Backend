package edu.ijse.inshiftbackend.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyAuthOptionsDTO {

    private String challenge;
    private List<CredentialDescriptorDTO> allowCredentials;

}