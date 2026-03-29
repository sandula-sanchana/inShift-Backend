package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.report.AttendanceReportFilterDTO;
import edu.ijse.inshiftbackend.dto.report.AttendanceReportResponseDTO;

public interface AttendanceReportService {
    AttendanceReportResponseDTO getAdminAttendanceReport(AttendanceReportFilterDTO filter);

    byte[] exportAdminAttendanceReportCsv(AttendanceReportFilterDTO filter);
}