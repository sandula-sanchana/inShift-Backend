package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.response.AdminEmployeeDeviceResponseDTO;

import java.util.List;

public interface AdminDeviceManagementService {
    List<AdminEmployeeDeviceResponseDTO> getEmployeeDevices(Long employeeId);
    void revokeDevice(Long deviceId, String reason);
    void restoreDevice(Long deviceId, String reason);
}