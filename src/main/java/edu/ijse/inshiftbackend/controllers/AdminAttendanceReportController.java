package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.report.AttendanceReportFilterDTO;
import edu.ijse.inshiftbackend.dto.report.AttendanceReportResponseDTO;
import edu.ijse.inshiftbackend.service.AttendanceReportService;
import edu.ijse.inshiftbackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/attendance-report")
@RequiredArgsConstructor
@CrossOrigin
public class AdminAttendanceReportController {

    private final AttendanceReportService attendanceReportService;

    @PostMapping
    public APIResponse<AttendanceReportResponseDTO> getReport(
            @RequestBody AttendanceReportFilterDTO filter
    ) {
        return new APIResponse<>(
                200,
                "Attendance report fetched successfully",
                attendanceReportService.getAdminAttendanceReport(filter)
        );
    }
}