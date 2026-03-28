package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.AdminCreateOvertimeDTO;
import edu.ijse.inshiftbackend.dto.EmployeeDeclineOvertimeDTO;
import edu.ijse.inshiftbackend.dto.EmployeeOfferOvertimeSwapDTO;
import edu.ijse.inshiftbackend.dto.response.OvertimeAssignmentResponseDTO;
import edu.ijse.inshiftbackend.dto.response.OvertimeSwapResponseDTO;

import java.util.List;

public interface OvertimeService {

    OvertimeAssignmentResponseDTO adminCreateOvertime(AdminCreateOvertimeDTO dto, String adminEmail);

    List<OvertimeAssignmentResponseDTO> getAllOvertimeAssignments();

    List<OvertimeAssignmentResponseDTO> getMyOvertimeAssignments(String employeeEmail);

    OvertimeAssignmentResponseDTO acceptOvertime(Long overtimeId, String employeeEmail);

    OvertimeAssignmentResponseDTO declineOvertime(Long overtimeId, EmployeeDeclineOvertimeDTO dto, String employeeEmail);

    OvertimeSwapResponseDTO offerSwap(Long overtimeId, EmployeeOfferOvertimeSwapDTO dto, String employeeEmail);

    List<OvertimeSwapResponseDTO> getIncomingSwapRequests(String employeeEmail);

    OvertimeSwapResponseDTO acceptSwap(Long swapRequestId, String employeeEmail);

    OvertimeSwapResponseDTO rejectSwap(Long swapRequestId, String employeeEmail);
}