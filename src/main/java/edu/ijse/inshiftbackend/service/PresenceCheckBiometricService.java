package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.PresenceCheckBiometricVerifyDTO;
import edu.ijse.inshiftbackend.dto.response.PresenceCheckBiometricProofDTO;

public interface PresenceCheckBiometricService {

    String getPresenceAssertionOptions(
            Long presenceCheckId,
            String deviceFingerprint,
            String employeeEmail
    );

    PresenceCheckBiometricProofDTO verifyPresenceAssertion(
            PresenceCheckBiometricVerifyDTO dto,
            String employeeEmail
    );
}