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
        List<PasskeyCredential> activeCredentials = passkeyCredentialRepository.findByEmployeeAndActiveTrue(employee);

        String normalizedDeviceName = normalize(deviceName);
        String normalizedUserAgent = normalize(userAgent);

        return activeCredentials.stream().anyMatch(c ->
                normalize(c.getDeviceName()).equals(normalizedDeviceName) &&
                        normalize(c.getUserAgent()).equals(normalizedUserAgent)
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}