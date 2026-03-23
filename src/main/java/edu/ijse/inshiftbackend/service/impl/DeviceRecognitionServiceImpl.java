package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.PasskeyCredential;
import edu.ijse.inshiftbackend.repository.PasskeyCredentialRepository;
import edu.ijse.inshiftbackend.service.DeviceRecognitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceRecognitionServiceImpl implements DeviceRecognitionService {

    private final PasskeyCredentialRepository passkeyCredentialRepository;

    @Override
    public boolean isSameDeviceLike(Employee employee, String deviceName, String userAgent) {
        if (employee == null) {
            return false;
        }

        String normalizedDeviceName = normalize(deviceName);
        String normalizedUserAgent = normalize(userAgent);

        if (normalizedDeviceName.isBlank() || normalizedUserAgent.isBlank()) {
            return false;
        }

        List<PasskeyCredential> activeCredentials =
                passkeyCredentialRepository.findByEmployeeAndActiveTrue(employee);

        if (activeCredentials == null || activeCredentials.isEmpty()) {
            return false;
        }

        return activeCredentials.stream().anyMatch(credential ->
                normalizedDeviceName.equals(normalize(credential.getDeviceName())) &&
                        normalizedUserAgent.equals(normalize(credential.getUserAgent()))
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}