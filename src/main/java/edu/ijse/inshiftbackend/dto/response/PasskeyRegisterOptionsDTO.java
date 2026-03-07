package edu.ijse.inshiftbackend.dto.response;

import java.util.List;

public class PasskeyRegisterOptionsDTO {

    private String challenge;
    private RpDTO rp;
    private UserDTO user;
    private List<PubKeyCredParamDTO> pubKeyCredParams;

}