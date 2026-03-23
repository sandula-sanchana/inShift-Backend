package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDevice;

public interface TrustedDeviceService {
    EmployeeDevice requireApprovedDevice(Employee employee, String deviceFingerprint);
    boolean hasApprovedCompanyPc(Employee employee);
    boolean hasApprovedMobile(Employee employee);
}