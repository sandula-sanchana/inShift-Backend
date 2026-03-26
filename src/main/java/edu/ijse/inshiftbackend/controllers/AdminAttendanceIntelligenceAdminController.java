package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.response.AttendanceIntelligenceOverviewDTO;
import edu.ijse.inshiftbackend.service.AttendanceIntelligenceService;
import edu.ijse.inshiftbackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/intelligence")
@CrossOrigin
public class AdminAttendanceIntelligenceAdminController {

    private final AttendanceIntelligenceService attendanceIntelligenceService;

    @GetMapping("/daily")
    public APIResponse<List<AttendanceIntelligenceOverviewDTO>> getDaily(
            @RequestParam(required = false) LocalDate date
    ) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        return new APIResponse<>(
                200,
                "Attendance intelligence overview fetched",
                attendanceIntelligenceService.getDailyOverview(target)
        );
    }
}