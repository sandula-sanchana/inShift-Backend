package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.PasskeyRegisterVerifyDTO;
import edu.ijse.inshiftbackend.dto.response.PasskeyRegisterOptionsDTO;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;
import edu.ijse.inshiftbackend.service.PasskeyService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/emp/passkey")
@CrossOrigin
public class EmployeePasskeyController {

    private final PasskeyService passkeyService;

    @PostMapping("/register/options")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<PasskeyRegisterOptionsDTO> getRegisterOptions() {
        return new APIResponse<>(
                200,
                "Passkey register options generated successfully",
                passkeyService.getPasskeyRegisterResponse(WebAuthnChallengePurpose.REGISTER)
        );
    }

    @PostMapping("/register/verify")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<Void> verifyAndSavePasskey(@RequestBody @Valid PasskeyRegisterVerifyDTO dto) {
        passkeyService.verifyAndSavePasskey(dto);
        return new APIResponse<>(
                200,
                "Passkey registered successfully",
                null
        );
    }
}