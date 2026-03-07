package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.response.PasskeyRegisterOptionsDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.WebAuthnChallenge;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;

public interface WebAuthnChallengeService {
   String createChallenge(WebAuthnChallengePurpose purpose, Employee employee);

    WebAuthnChallenge getValidChallenge(WebAuthnChallengePurpose purpose);

    void markChallengeAsUsed(WebAuthnChallenge challenge);
}
