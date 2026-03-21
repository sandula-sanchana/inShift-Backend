package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.PresenceCheckCreateDTO;
import edu.ijse.inshiftbackend.dto.response.PresenceCheckResponseDTO;
import edu.ijse.inshiftbackend.service.PresenceCheckService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/presence-check")
@RequiredArgsConstructor
@CrossOrigin
public class AdminPresenceCheckController {

    private final PresenceCheckService presenceCheckService;

    @PostMapping("/trigger")
    public APIResponse<PresenceCheckResponseDTO> trigger(
            @RequestBody @Valid PresenceCheckCreateDTO dto,
            Authentication auth
    ) {
        return new APIResponse<>(
                200,
                "Presence check triggered successfully",
                presenceCheckService.createPresenceCheck(dto, auth.getName())
        );
    }

    @GetMapping("/active")
    public APIResponse<List<PresenceCheckResponseDTO>> active() {
        return new APIResponse<>(
                200,
                "Active presence checks fetched successfully",
                presenceCheckService.getActivePresenceChecks()
        );
    }

    @GetMapping("/history")
    public APIResponse<List<PresenceCheckResponseDTO>> history() {
        return new APIResponse<>(
                200,
                "Presence check history fetched successfully",
                presenceCheckService.getPresenceCheckHistory()
        );
    }
}