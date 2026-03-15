package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.AttendanceCorrectionRequestDTO;
import edu.ijse.inshiftbackend.dto.CorrectionDecisionDTO;
import edu.ijse.inshiftbackend.dto.response.AttendanceCorrectionResponseDTO;

import java.util.List;

public interface AttendanceCorrectionService {

    AttendanceCorrectionResponseDTO submitRequest(AttendanceCorrectionRequestDTO dto, String employeeEmail);

    List<AttendanceCorrectionResponseDTO> getMyRequests(String employeeEmail);

    List<AttendanceCorrectionResponseDTO> getPendingRequests();

    AttendanceCorrectionResponseDTO approveRequest(Long requestId, CorrectionDecisionDTO dto, String adminEmail);

    AttendanceCorrectionResponseDTO rejectRequest(Long requestId, CorrectionDecisionDTO dto, String adminEmail);
}