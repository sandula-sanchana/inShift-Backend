package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.CorrectionDecisionDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceCorrectionResponseDTO;
import edu.ijse.inshiftbackend.service.AttendanceCorrectionService;
import edu.ijse.inshiftbackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/attendance-corrections")
@RequiredArgsConstructor
@CrossOrigin
public class AdminAttendanceCorrectionController {

    private final AttendanceCorrectionService service;

    @GetMapping("/pending")
    public APIResponse<List<AttendanceCorrectionResponseDTO>> getPending() {
        return new APIResponse<>(200, "Pending correction requests fetched", service.getPendingRequests());
    }

    @PutMapping("/{id}/approve")
    public APIResponse<AttendanceCorrectionResponseDTO> approve(@PathVariable Long id, @RequestBody CorrectionDecisionDTO dto, Authentication auth) {
        return new APIResponse<>(200, "Correction request approved", service.approveRequest(id, dto, auth.getName()));
    }

    @PutMapping("/{id}/reject")
    public APIResponse<AttendanceCorrectionResponseDTO> reject(@PathVariable Long id, @RequestBody CorrectionDecisionDTO dto, Authentication auth) {
        return new APIResponse<>(200, "Correction request rejected", service.rejectRequest(id, dto, auth.getName()));
    }
}