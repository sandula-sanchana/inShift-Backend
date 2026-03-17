package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.entity.Employee;

public interface DeviceRecognitionService {
    boolean isSameDeviceLike(Employee employee, String deviceName, String userAgent);
}