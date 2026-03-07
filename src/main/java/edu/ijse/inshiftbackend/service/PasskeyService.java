package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.response.PasskeyRegisterOptionsDTO;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;

public interface PasskeyService {

    PasskeyRegisterOptionsDTO getPasskeyRegisterResponse(WebAuthnChallengePurpose purpose);
}
