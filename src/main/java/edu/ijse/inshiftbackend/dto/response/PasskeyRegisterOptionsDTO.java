package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyRegisterOptionsDTO {

    private RpDTO rp;
    private UserDTO user;
    private String challenge;

    private List<PubKeyCredParamDTO> pubKeyCredParams;

    private Integer timeout;
    private String attestation;

    // simplified authenticatorSelection fields
    private String authenticatorAttachment; // platform -> device biometrics
    private String residentKey;             // preferred / required
    private String userVerification;        // required  ->  for biomatrics
}