package edu.ijse.inshiftbackend.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyRegisterOptionsDTO {

    private String challenge;
    private RpDTO rp;
    private UserDTO user;
    private List<PubKeyCredParamDTO> pubKeyCredParams;

}