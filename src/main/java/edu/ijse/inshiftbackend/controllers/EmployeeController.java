package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.ChangePasswordDTO;
import edu.ijse.inshiftbackend.dto.EmployeeDTO;
import edu.ijse.inshiftbackend.service.EmployeeService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/emp")
@CrossOrigin
public class EmployeeController {
    private final EmployeeService employeeService;

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<EmployeeDTO> me() {
        return new APIResponse<>(200, "OK", employeeService.getMe());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<Void> changePassword(@RequestBody @Valid ChangePasswordDTO dto) {
        employeeService.changeMyPassword(dto);
        return new APIResponse<>(200, "Password updated successfully", null);
    }
}
