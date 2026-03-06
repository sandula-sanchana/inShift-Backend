package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.AttendancePunchDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceResponseDTO;
import edu.ijse.inshiftbackend.entity.enums.AttendanceSource;
import edu.ijse.inshiftbackend.service.AttendanceService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/emp/attendance")
@CrossOrigin
public class AttendanceController {

    private final AttendanceService attendanceService;

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
}