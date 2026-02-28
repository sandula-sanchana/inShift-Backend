package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.EmployeeDTO;
import edu.ijse.inshiftbackend.service.EmployeeService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/employees")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminEmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public APIResponse<Map<String, Object>> createEmployee(@RequestBody @Valid EmployeeDTO employeeDTO) {

        String tempPassword = employeeService.saveEmployee(employeeDTO);

        return new APIResponse<>(
                201,
                "Employee Saved Successfully",
                Map.of("tempPassword", tempPassword)
        );
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<List<EmployeeDTO>> getAllEmployees() {
        return new APIResponse<>(
                200,
                "OK",
                employeeService.getAllEmployees()
        );
    }

}