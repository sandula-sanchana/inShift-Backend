package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.AttendanceCorrectionRequestDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceCorrectionResponseDTO;
import edu.ijse.inshiftbackend.service.AttendanceCorrectionService;
import edu.ijse.inshiftbackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/emp/attendance-corrections")
@RequiredArgsConstructor
@CrossOrigin
public class EmployeeAttendanceCorrectionController {

    private final AttendanceCorrectionService service;

    @PostMapping
    public APIResponse<AttendanceCorrectionResponseDTO> submit(@RequestBody AttendanceCorrectionRequestDTO dto, Authentication auth) {
        return new APIResponse<>(200, "Correction request submitted", service.submitRequest(dto, auth.getName()));
    }

    @GetMapping("/my")
    public APIResponse<List<AttendanceCorrectionResponseDTO>> myRequests(Authentication auth) {
        return new APIResponse<>(200, "My correction requests fetched", service.getMyRequests(auth.getName()));
    }
}