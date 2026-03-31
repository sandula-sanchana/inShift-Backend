package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.response.CompanyAiScannerDashboardDTO;
import edu.ijse.inshiftbackend.dto.response.EmployeeAiScannerDetailDTO;

public interface CompanyAiPatternScannerService {
    CompanyAiScannerDashboardDTO runCompanyScan(int days);
    EmployeeAiScannerDetailDTO getEmployeeScannerDetail(Long employeeId, int days);
}