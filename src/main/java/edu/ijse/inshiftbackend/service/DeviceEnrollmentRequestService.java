package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.response.AdminDeviceEnrollmentRequestResponseDTO;
import edu.ijse.inshiftbackend.entity.DeviceEnrollmentRequest;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;

import java.util.List;

public interface DeviceEnrollmentRequestService {

    DeviceEnrollmentRequest createPendingReplacementRequest(
            Employee employee,
            String deviceFingerprint,
            String deviceName,
            String userAgent,
            String ipAddress,
            DeviceTrustType requestedTrustType
    );

    DeviceEnrollmentRequest getApprovedValidRequest(Employee employee);

    void expireOldPendingRequests(Employee employee);

    void approveRequest(Long requestId, DeviceTrustType approvedTrustType, String adminComment);

    void rejectRequest(Long requestId, String adminComment);

    List<AdminDeviceEnrollmentRequestResponseDTO> getPendingRequests();
}