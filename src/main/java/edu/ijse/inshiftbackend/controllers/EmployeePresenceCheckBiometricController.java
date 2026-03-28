package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.PresenceCheckBiometricVerifyDTO;
import edu.ijse.inshiftbackend.dto.response.PresenceCheckBiometricProofDTO;
import edu.ijse.inshiftbackend.service.PresenceCheckBiometricService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/emp/presence-check/biometric")
@RequiredArgsConstructor
@CrossOrigin
public class EmployeePresenceCheckBiometricController {

    private final PresenceCheckBiometricService presenceCheckBiometricService;

    @PostMapping("/options")
    public APIResponse<String> getOptions(
            @RequestParam Long presenceCheckId,
            @RequestParam String deviceFingerprint,
            Authentication auth
    ) {
        return new APIResponse<>(
                200,
                "Presence biometric options fetched successfully",
                presenceCheckBiometricService.getPresenceAssertionOptions(
                        presenceCheckId,
                        deviceFingerprint,
                        auth.getName()
                )
        );
    }

    @PostMapping("/verify")
    public APIResponse<PresenceCheckBiometricProofDTO> verify(
            @RequestBody @Valid PresenceCheckBiometricVerifyDTO dto,
            Authentication auth
    ) {
        return new APIResponse<>(
                200,
                "Presence biometric verified successfully",
                presenceCheckBiometricService.verifyPresenceAssertion(dto, auth.getName())
        );
    }
}