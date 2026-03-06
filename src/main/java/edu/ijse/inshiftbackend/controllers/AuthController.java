package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.AuthDTO;
import edu.ijse.inshiftbackend.dto.response.AuthResponseDTO;
import edu.ijse.inshiftbackend.service.AuthService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<AuthResponseDTO> login(@RequestBody @Valid AuthDTO authDTO) {

        AuthResponseDTO response = authService.login(authDTO);

        return new APIResponse<>(
                HttpStatus.OK.value(),
                "Login Successful",
                 response
        );
    }
}