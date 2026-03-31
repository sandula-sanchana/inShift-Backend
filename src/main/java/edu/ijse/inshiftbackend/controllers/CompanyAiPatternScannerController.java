package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.response.CompanyAiScannerDashboardDTO;
import edu.ijse.inshiftbackend.dto.response.EmployeeAiScannerDetailDTO;
import edu.ijse.inshiftbackend.service.CompanyAiPatternScannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/ai-pattern-scanner")
@RequiredArgsConstructor
public class CompanyAiPatternScannerController {

    private final CompanyAiPatternScannerService companyAiPatternScannerService;

    @PostMapping("/run")
    public ResponseEntity<CompanyAiScannerDashboardDTO> runScan(
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(companyAiPatternScannerService.runCompanyScan(days));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<EmployeeAiScannerDetailDTO> getEmployeeDetail(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(
                companyAiPatternScannerService.getEmployeeScannerDetail(employeeId, days)
        );
    }
}