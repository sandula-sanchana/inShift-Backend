package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.PresenceCheckCreateDTO;
import edu.ijse.inshiftbackend.dto.EmpPresenceCheckRespondDTO;
import edu.ijse.inshiftbackend.dto.response.PresenceCheckResponseDTO;

import java.util.List;

public interface PresenceCheckService {

    PresenceCheckResponseDTO createPresenceCheck(PresenceCheckCreateDTO dto, String adminEmail);

    PresenceCheckResponseDTO getCurrentPendingForEmployee(String employeeEmail);

    PresenceCheckResponseDTO respondToPresenceCheck(EmpPresenceCheckRespondDTO dto, String employeeEmail);

    List<PresenceCheckResponseDTO> getActivePresenceChecks();

    List<PresenceCheckResponseDTO> getPresenceCheckHistory();

    List<PresenceCheckResponseDTO> getMyPresenceCheckHistory(String employeeEmail);
}