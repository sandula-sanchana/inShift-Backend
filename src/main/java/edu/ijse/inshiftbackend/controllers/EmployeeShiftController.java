package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.ShiftDTO;
import edu.ijse.inshiftbackend.service.ShiftService;
import edu.ijse.inshiftbackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/emp/shifts")
@RequiredArgsConstructor
@CrossOrigin
public class EmployeeShiftController {

    private final ShiftService shiftService;

    @GetMapping("/default")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<ShiftDTO> getDefaultShift() {
        return new APIResponse<>(
                200,
                "Employee shift fetched successfully",
                shiftService.getDefaultShift()
        );
    }
}