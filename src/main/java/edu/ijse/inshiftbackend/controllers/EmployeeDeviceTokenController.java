package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.DeviceFCMTokenRegisterDTO;
import edu.ijse.inshiftbackend.dto.response.EmployeeDeviceTokenResponseDTO;
import edu.ijse.inshiftbackend.service.EmployeeDeviceTokenService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/emp/device-tokens")
@RequiredArgsConstructor
@CrossOrigin
public class EmployeeDeviceTokenController {

    private final EmployeeDeviceTokenService employeeDeviceTokenService;

    @PostMapping("/register")
    public APIResponse<EmployeeDeviceTokenResponseDTO> register(
            @RequestBody @Valid DeviceFCMTokenRegisterDTO dto,
            Authentication auth
    ) {
        return new APIResponse<>(
                200,
                "Device token registered successfully",
                employeeDeviceTokenService.registerToken(dto, auth.getName())
        );
    }

    @GetMapping("/my")
    public APIResponse<List<EmployeeDeviceTokenResponseDTO>> myTokens(Authentication auth) {
        return new APIResponse<>(
                200,
                "My active device tokens fetched successfully",
                employeeDeviceTokenService.getMyActiveTokens(auth.getName())
        );
    }

    @PostMapping("/deactivate")
    public APIResponse<Void> deactivate(
            @RequestBody Map<String, String> body,
            Authentication auth
    ) {
        employeeDeviceTokenService.deactivateMyToken(body.get("fcmToken"), auth.getName());

        return new APIResponse<>(
                200,
                "Device token deactivated successfully",
                null
        );
    }
}