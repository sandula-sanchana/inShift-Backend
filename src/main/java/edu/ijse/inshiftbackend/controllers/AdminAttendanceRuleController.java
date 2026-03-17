package edu.ijse.inshiftbackend.controllers.admin;

import edu.ijse.inshiftbackend.dto.AttendanceRuleDTO;
import edu.ijse.inshiftbackend.dto.AttendanceRuleUpdateDTO;
import edu.ijse.inshiftbackend.service.AttendanceRuleService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/attendance-rules")
@CrossOrigin
public class AdminAttendanceRuleController {

    private final AttendanceRuleService attendanceRuleService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<List<AttendanceRuleDTO>> getAllRules() {
        return new APIResponse<>(
                200,
                "Attendance rules fetched successfully",
                attendanceRuleService.getAllRules()
        );
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<AttendanceRuleDTO> updateRule(
            @PathVariable Long id,
            @RequestBody @Valid AttendanceRuleUpdateDTO dto
    ) {
        return new APIResponse<>(
                200,
                "Attendance rule updated successfully",
                attendanceRuleService.updateRule(id, dto)
        );
    }
}