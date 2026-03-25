package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.response.EmployeeAiRiskAnalysisResponseDTO;

public interface AdminAiRiskService {
    EmployeeAiRiskAnalysisResponseDTO getEmployeeAiRiskAnalysis(Long employeeId);
}