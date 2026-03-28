package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.EmpPresenceCheckRespondDTO;
import edu.ijse.inshiftbackend.dto.response.PresenceCheckResponseDTO;
import edu.ijse.inshiftbackend.service.PresenceCheckService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/emp/presence-check")
@RequiredArgsConstructor
@CrossOrigin
public class EmployeePresenceCheckController {

    private final PresenceCheckService presenceCheckService;

    @GetMapping("/current")
    public APIResponse<PresenceCheckResponseDTO> current(Authentication auth) {
        return new APIResponse<>(
                200,
                "Current presence check fetched successfully",
                presenceCheckService.getCurrentPendingForEmployee(auth.getName())
        );
    }

    @GetMapping("/{presenceCheckId}")
    public APIResponse<PresenceCheckResponseDTO> getById(
            @PathVariable Long presenceCheckId,
            Authentication auth
    ) {
        return new APIResponse<>(
                200,
                "Presence check fetched successfully",
                presenceCheckService.getPresenceCheckByIdForEmployee(presenceCheckId, auth.getName())
        );
    }

    @PostMapping("/respond")
    public APIResponse<PresenceCheckResponseDTO> respond(
            @RequestBody @Valid EmpPresenceCheckRespondDTO dto,
            Authentication auth
    ) {
        return new APIResponse<>(
                200,
                "Presence check responded successfully",
                presenceCheckService.respondToPresenceCheck(dto, auth.getName())
        );
    }

    @GetMapping("/my-history")
    public APIResponse<List<PresenceCheckResponseDTO>> myHistory(Authentication auth) {
        return new APIResponse<>(
                200,
                "My presence check history fetched successfully",
                presenceCheckService.getMyPresenceCheckHistory(auth.getName())
        );
    }
}