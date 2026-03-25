package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.response.EmployeeAiRiskAnalysisResponseDTO;
import edu.ijse.inshiftbackend.service.AdminAiRiskService;
import edu.ijse.inshiftbackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/ai-risk")
@CrossOrigin
public class AdminAiRiskController {

    private final AdminAiRiskService adminAiRiskService;

    @GetMapping("/employee/{employeeId}")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<EmployeeAiRiskAnalysisResponseDTO> getEmployeeAiRiskAnalysis(
            @PathVariable Long employeeId
    ) {
        return new APIResponse<>(
                200,
                "Employee AI risk analysis fetched successfully",
                adminAiRiskService.getEmployeeAiRiskAnalysis(employeeId)
        );
    }
}