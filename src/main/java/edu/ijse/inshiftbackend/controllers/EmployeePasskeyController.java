package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.PasskeyAssertionVerifyDTO;
import edu.ijse.inshiftbackend.dto.PasskeyRegisterStartDTO;
import edu.ijse.inshiftbackend.dto.PasskeyRegisterVerifyDTO;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;
import edu.ijse.inshiftbackend.service.PasskeyService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
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
    public APIResponse<String> getRegisterOptions(
            @RequestBody @Valid PasskeyRegisterStartDTO dto,
            HttpServletRequest request
    ) {
        return new APIResponse<>(
                200,
                "Passkey register options generated successfully",
                passkeyService.getPasskeyRegisterResponse(
                        WebAuthnChallengePurpose.REGISTER,
                        dto,
                        request.getHeader("User-Agent"),
                        request.getRemoteAddr()
                )
        );
    }

    @PostMapping("/register/verify")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<Void> verifyAndSavePasskey(
            @RequestBody @Valid PasskeyRegisterVerifyDTO dto,
            HttpServletRequest request
    ) {
        passkeyService.verifyAndSavePasskey(dto, request.getHeader("User-Agent"));
        return new APIResponse<>(
                200,
                "Passkey registered successfully",
                null
        );
    }

    @PostMapping("/assertion/options")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<String> getAssertionOptions() {
        return new APIResponse<>(
                200,
                "Passkey assertion options generated successfully",
                passkeyService.getPasskeyAssertionResponse(WebAuthnChallengePurpose.AUTHENTICATE)
        );
    }

    @PostMapping("/assertion/verify")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<Void> verifyAssertion(@RequestBody @Valid PasskeyAssertionVerifyDTO dto) {
        passkeyService.verifyPasskeyAssertion(dto);
        return new APIResponse<>(
                200,
                "Passkey verified successfully",
                null
        );
    }
}