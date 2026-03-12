package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.PasskeyAssertionVerifyDTO;
import edu.ijse.inshiftbackend.dto.PasskeyRegisterVerifyDTO;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;

public interface PasskeyService {

    String getPasskeyRegisterResponse(WebAuthnChallengePurpose purpose);

    void verifyAndSavePasskey(PasskeyRegisterVerifyDTO dto);

    String getPasskeyAssertionResponse(WebAuthnChallengePurpose purpose);

    void verifyPasskeyAssertion(PasskeyAssertionVerifyDTO dto);
}
