package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.AttendanceDecisionDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceDailySummaryResponseDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceResponseDTO;
import edu.ijse.inshiftbackend.service.AttendanceService;
import edu.ijse.inshiftbackend.service.AttendanceSummaryService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/attendance")
@CrossOrigin
public class AdminAttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceSummaryService attendanceSummaryService;

    @GetMapping("/pending")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<List<AttendanceResponseDTO>> pending() {
        return new APIResponse<>(200, "Pending attendance list", attendanceService.getPending());
    }

    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<AttendanceResponseDTO> approve(@PathVariable Long id) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return new APIResponse<>(200, "Attendance approved", attendanceService.approve(id, adminEmail));
    }

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<AttendanceResponseDTO> reject(
            @PathVariable Long id,
            @RequestBody @Valid AttendanceDecisionDTO dto
    ) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return new APIResponse<>(200, "Attendance rejected", attendanceService.reject(id, dto, adminEmail));
    }

    @GetMapping("/summary/employee/{employeeId}")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<AttendanceDailySummaryResponseDTO> getEmployeeSummary(
            @PathVariable Long employeeId,
            @RequestParam LocalDate date
    ) {
        AttendanceDailySummaryResponseDTO res =
                attendanceSummaryService.generateDailySummary(employeeId, date);

        return new APIResponse<>(200, "Employee attendance summary", res);
    }
}