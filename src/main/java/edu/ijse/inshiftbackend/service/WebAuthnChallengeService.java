package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;

public interface WebAuthnChallengeService {
   void createChallenge(WebAuthnChallengePurpose purpose);
}
