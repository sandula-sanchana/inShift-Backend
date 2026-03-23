package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.DeviceEnrollRequestDTO;
import edu.ijse.inshiftbackend.dto.response.DeviceEnrollResponseDTO;

public interface EmployeeDeviceService {
    DeviceEnrollResponseDTO enrollMyDevice(DeviceEnrollRequestDTO dto, String employeeEmail);
}