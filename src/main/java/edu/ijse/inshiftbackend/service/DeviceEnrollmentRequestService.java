package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.entity.DeviceEnrollmentRequest;
import edu.ijse.inshiftbackend.entity.Employee;

public interface DeviceEnrollmentRequestService {

    DeviceEnrollmentRequest createPendingReplacementRequest(
            Employee employee,
            String deviceName,
            String userAgent,
            String ipAddress
    );

    DeviceEnrollmentRequest getApprovedValidRequest(Employee employee);

    void expireOldPendingRequests(Employee employee);

    void approveRequest(Long requestId, String adminComment);

    void rejectRequest(Long requestId, String adminComment);
}