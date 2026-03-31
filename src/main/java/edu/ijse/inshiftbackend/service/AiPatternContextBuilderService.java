package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.response.EmployeeAiPatternContextDTO;

import java.util.List;

public interface AiPatternContextBuilderService {
    EmployeeAiPatternContextDTO buildEmployeeContext(Long employeeId, int days);
    List<EmployeeAiPatternContextDTO> buildAllEmployeeContexts(int days);
}