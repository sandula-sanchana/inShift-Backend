package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.AdminCreateOvertimeDTO;
import edu.ijse.inshiftbackend.dto.response.OvertimeAssignmentResponseDTO;
import edu.ijse.inshiftbackend.service.OvertimeService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/ot")
@RequiredArgsConstructor
@CrossOrigin
public class AdminOvertimeController {

    private final OvertimeService overtimeService;

    @PostMapping
    public APIResponse<OvertimeAssignmentResponseDTO> create(
            @RequestBody @Valid AdminCreateOvertimeDTO dto,
            Authentication auth
    ) {
        return new APIResponse<>(
                200,
                "OT assigned successfully",
                overtimeService.adminCreateOvertime(dto, auth.getName())
        );
    }

    @GetMapping
    public APIResponse<List<OvertimeAssignmentResponseDTO>> getAll() {
        return new APIResponse<>(
                200,
                "All OT assignments fetched successfully",
                overtimeService.getAllOvertimeAssignments()
        );
    }
}