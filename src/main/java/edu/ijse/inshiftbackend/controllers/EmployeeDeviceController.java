package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.DeviceEnrollRequestDTO;
import edu.ijse.inshiftbackend.dto.response.DeviceEnrollResponseDTO;
import edu.ijse.inshiftbackend.service.EmployeeDeviceService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/emp/device")
@CrossOrigin
public class EmployeeDeviceController {

    private final EmployeeDeviceService employeeDeviceService;

    @PostMapping("/enroll")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<DeviceEnrollResponseDTO> enrollMyDevice(
            @Valid @RequestBody DeviceEnrollRequestDTO dto
    ) {
        String employeeEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        return new APIResponse<>(
                200,
                "Device enrollment processed successfully",
                employeeDeviceService.enrollMyDevice(dto, employeeEmail)
        );
    }
}