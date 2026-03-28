package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.PasskeyAssertionVerifyDTO;
import edu.ijse.inshiftbackend.dto.PasskeyRegisterStartDTO;
import edu.ijse.inshiftbackend.dto.PasskeyRegisterVerifyDTO;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;

public interface PasskeyService {

    String getPasskeyRegisterResponse(
            WebAuthnChallengePurpose purpose,
            PasskeyRegisterStartDTO dto,
            String userAgent,
            String ipAddress
    );

    void verifyAndSavePasskey(PasskeyRegisterVerifyDTO dto, String userAgent);

    String getPasskeyAssertionResponse(WebAuthnChallengePurpose purpose);

    void verifyPasskeyAssertion(PasskeyAssertionVerifyDTO dto);

    void verifyPasskeyAssertion(PasskeyAssertionVerifyDTO dto, WebAuthnChallengePurpose purpose);
}