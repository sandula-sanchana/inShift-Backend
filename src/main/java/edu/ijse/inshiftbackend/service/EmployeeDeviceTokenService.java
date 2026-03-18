package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.DeviceFCMTokenRegisterDTO;
import edu.ijse.inshiftbackend.dto.response.EmployeeDeviceTokenResponseDTO;

import java.util.List;

public interface EmployeeDeviceTokenService {

    EmployeeDeviceTokenResponseDTO registerToken(DeviceFCMTokenRegisterDTO dto, String email);

    void deactivateMyToken(String fcmToken, String email);

    List<EmployeeDeviceTokenResponseDTO> getMyActiveTokens(String email);
}