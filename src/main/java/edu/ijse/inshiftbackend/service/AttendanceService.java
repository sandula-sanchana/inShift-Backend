package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.AttendancePunchDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceResponseDTO;
import edu.ijse.inshiftbackend.entity.enums.AttendanceSource;

import java.util.List;

public interface AttendanceService {
    AttendanceResponseDTO punch(
            AttendancePunchDTO dto,
            AttendanceSource source,
            String email
    );

    List<AttendanceResponseDTO> getPending();

    AttendanceResponseDTO approve(Long attendanceId, String adminEmail);

    AttendanceResponseDTO reject(Long attendanceId, AttendanceDecisionDTO dto, String adminEmail);
}