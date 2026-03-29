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
@CrossOrigin
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

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<EmployeeDTO> getEmployeeById(@PathVariable Long id) {
        return new APIResponse<>(
                200,
                "Employee fetched successfully",
                employeeService.getEmployeeById(id)
        );
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<String> updateEmployee(
            @PathVariable Long id,
            @RequestBody @Valid EmployeeDTO employeeDTO
    ) {
        employeeService.updateEmployee(id, employeeDTO);

        return new APIResponse<>(
                200,
                "Employee updated successfully",
                null
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<String> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);

        return new APIResponse<>(
                200,
                "Employee deleted successfully",
                null
        );
    }

    @GetMapping("/search")
    public APIResponse<List<EmployeeDTO>> searchEmployees(@RequestParam("q") String query) {
        String q = query == null ? "" : query.trim().toLowerCase();

        List<EmployeeDTO> results = employeeService.getAllEmployees()
                .stream()
                .filter(EmployeeDTO::getActive)
                .filter(emp ->
                        (emp.getEmpCode() != null && emp.getEmpCode().toLowerCase().contains(q)) ||
                                (emp.getFullName() != null && emp.getFullName().toLowerCase().contains(q))
                )
                .limit(10)
                .toList();

        return new APIResponse<>(
                200,
                "Employees fetched successfully",
                results
        );
    }
}