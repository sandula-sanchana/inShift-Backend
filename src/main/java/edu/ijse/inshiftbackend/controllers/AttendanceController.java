package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.AttendancePunchDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceDailySummaryResponseDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceResponseDTO;
import edu.ijse.inshiftbackend.entity.enums.AttendanceSource;
import edu.ijse.inshiftbackend.service.AttendanceService;
import edu.ijse.inshiftbackend.service.AttendanceSummaryService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/emp/attendance")
@CrossOrigin
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceSummaryService attendanceSummaryService;

    //MOBILE punch (GPS required, saved as VALID)
    @PostMapping("/punch/mobile")
    @ResponseStatus(HttpStatus.CREATED)
    public APIResponse<AttendanceResponseDTO> punchMobile(@RequestBody @Valid AttendancePunchDTO dto) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        AttendanceResponseDTO res = attendanceService.punch(dto, AttendanceSource.MOBILE, email);

        return new APIResponse<>(201, "Attendance saved (MOBILE)", res);
    }

    //WEB punch (Reason required, saved as PENDING)
    @PostMapping("/punch/web")
    @ResponseStatus(HttpStatus.CREATED)
    public APIResponse<AttendanceResponseDTO> punchWeb(@RequestBody @Valid AttendancePunchDTO dto) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        AttendanceResponseDTO res = attendanceService.punch(dto, AttendanceSource.WEB, email);

        return new APIResponse<>(201, "Attendance submitted (WEB) - Pending approval", res);
    }

    @GetMapping("/summary/today")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<AttendanceDailySummaryResponseDTO> getTodaySummary() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        AttendanceDailySummaryResponseDTO res =
                attendanceSummaryService.getTodaySummaryByEmail(email);

        return new APIResponse<>(200, "Today attendance summary", res);
    }

    @GetMapping("/summary")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<AttendanceDailySummaryResponseDTO> getSummaryByDate(
            @RequestParam LocalDate date
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        AttendanceDailySummaryResponseDTO res =
                attendanceSummaryService.getSummaryByEmailAndDate(email, date);

        return new APIResponse<>(200, "Attendance summary", res);
    }
}