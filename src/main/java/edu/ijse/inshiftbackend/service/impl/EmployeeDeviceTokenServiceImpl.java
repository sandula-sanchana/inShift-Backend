package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.DeviceFCMTokenRegisterDTO;
import edu.ijse.inshiftbackend.dto.response.EmployeeDeviceTokenResponseDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDeviceToken;
import edu.ijse.inshiftbackend.entity.enums.DeviceType;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeDeviceFCMTokenRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.EmployeeDeviceTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmployeeDeviceTokenServiceImpl implements EmployeeDeviceTokenService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeDeviceFCMTokenRepository employeeDeviceTokenRepository;

    @Override
    @Transactional
    public EmployeeDeviceTokenResponseDTO registerToken(DeviceFCMTokenRegisterDTO dto, String email) {
        if (dto == null || dto.getFcmToken() == null || dto.getFcmToken().isBlank()) {
            throw new BadRequestException("FCM token is required");
        }

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        DeviceType deviceType = parseDeviceType(dto.getDeviceType());
        String token = dto.getFcmToken().trim();
        String deviceName = normalize(dto.getDeviceName());
        String userAgent = normalize(dto.getUserAgent());

        EmployeeDeviceToken tokenEntity = employeeDeviceTokenRepository.findByFcmToken(token)
                .orElse(null);

        if (tokenEntity != null) {
            tokenEntity.setEmployee(employee);
            tokenEntity.setDeviceType(deviceType);
            tokenEntity.setDeviceName(deviceName);
            tokenEntity.setUserAgent(userAgent);
            tokenEntity.setActive(true);
            tokenEntity.setLastUsedAt(LocalDateTime.now());

            return map(employeeDeviceTokenRepository.save(tokenEntity));
        }

        EmployeeDeviceToken sameDevice = employeeDeviceTokenRepository
                .findByEmployeeAndDeviceTypeAndDeviceNameAndUserAgent(
                        employee,
                        deviceType,
                        deviceName,
                        userAgent
                )
                .orElse(null);

        if (sameDevice != null) {
            sameDevice.setFcmToken(token);
            sameDevice.setActive(true);
            sameDevice.setLastUsedAt(LocalDateTime.now());

            return map(employeeDeviceTokenRepository.save(sameDevice));
        }

        EmployeeDeviceToken created = EmployeeDeviceToken.builder()
                .employee(employee)
                .fcmToken(token)
                .deviceType(deviceType)
                .deviceName(deviceName)
                .userAgent(userAgent)
                .active(true)
                .lastUsedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        return map(employeeDeviceTokenRepository.save(created));
    }

    @Override
    @Transactional
    public void deactivateMyToken(String fcmToken, String email) {
        if (fcmToken == null || fcmToken.isBlank()) {
            throw new BadRequestException("FCM token is required");
        }

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        EmployeeDeviceToken tokenEntity = employeeDeviceTokenRepository.findByFcmToken(fcmToken.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Device token not found"));

        if (!tokenEntity.getEmployee().getEmployeeId().equals(employee.getEmployeeId())) {
            throw new BadRequestException("You can only deactivate your own device token");
        }

        tokenEntity.setActive(false);
        tokenEntity.setLastUsedAt(LocalDateTime.now());

        employeeDeviceTokenRepository.save(tokenEntity);
    }

    @Override
    public List<EmployeeDeviceTokenResponseDTO> getMyActiveTokens(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        return employeeDeviceTokenRepository.findAllByEmployeeAndActiveTrue(employee)
                .stream()
                .map(this::map)
                .toList();
    }

    private DeviceType parseDeviceType(String value) {
        try {
            return DeviceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("Invalid device type");
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private EmployeeDeviceTokenResponseDTO map(EmployeeDeviceToken entity) {
        return EmployeeDeviceTokenResponseDTO.builder()
                .id(entity.getId())
                .deviceType(entity.getDeviceType().name())
                .deviceName(entity.getDeviceName())
                .userAgent(entity.getUserAgent())
                .active(entity.isActive())
                .lastUsedAt(entity.getLastUsedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}