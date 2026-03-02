package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.AttendancePunchDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceResponseDTO;
import edu.ijse.inshiftbackend.entity.enums.AttendanceSource;

public interface AttendanceService {
    AttendanceResponseDTO punch(
            AttendancePunchDTO dto,
            AttendanceSource source,
            String email
    );
}