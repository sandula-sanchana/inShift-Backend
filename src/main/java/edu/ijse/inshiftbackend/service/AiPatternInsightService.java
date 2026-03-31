package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.response.CompanyAiOverviewDTO;
import edu.ijse.inshiftbackend.dto.response.EmployeeAiPatternContextDTO;
import edu.ijse.inshiftbackend.dto.response.EmployeeAiPatternInsightDTO;
import edu.ijse.inshiftbackend.dto.response.SuspiciousEmployeeAiCardDTO;

import java.util.List;

public interface AiPatternInsightService {
    EmployeeAiPatternInsightDTO analyzeEmployee(EmployeeAiPatternContextDTO context);
    CompanyAiOverviewDTO analyzeCompany(
            List<EmployeeAiPatternContextDTO> contexts,
            List<SuspiciousEmployeeAiCardDTO> suspiciousEmployees
    );
}