package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.AdminNotificationTestDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDeviceToken;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeDeviceFCMTokenRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.service.AdminNotificationService;
import edu.ijse.inshiftbackend.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeDeviceFCMTokenRepository employeeDeviceFCMTokenRepository;
    private final PushNotificationService pushNotificationService;

    @Override
    public void sendTestNotification(AdminNotificationTestDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Notification test request is required");
        }

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        List<EmployeeDeviceToken> activeTokens =
                employeeDeviceFCMTokenRepository.findAllByEmployeeAndActiveTrue(employee);

        if (activeTokens == null || activeTokens.isEmpty()) {
            throw new BadRequestException("No active notification tokens found for this employee");
        }

        String title = dto.getTitle() != null && !dto.getTitle().isBlank()
                ? dto.getTitle()
                : "InShift Test Notification";

        String body = dto.getBody() != null && !dto.getBody().isBlank()
                ? dto.getBody()
                : "Push notification is working correctly.";

        Map<String, String> data = new HashMap<>();
        data.put("type", "TEST_NOTIFICATION");
        data.put("url", "/emp/notifications");
        data.put("sentAt", LocalDateTime.now().toString());

        boolean sentAtLeastOne = false;

        for (EmployeeDeviceToken tokenEntity : activeTokens) {
            if (tokenEntity == null || tokenEntity.getFcmToken() == null || tokenEntity.getFcmToken().isBlank()) {
                continue;
            }

            try {
                pushNotificationService.sendToToken(
                        tokenEntity.getFcmToken(),
                        title,
                        body,
                        data
                );

                tokenEntity.setLastUsedAt(LocalDateTime.now());
                employeeDeviceFCMTokenRepository.save(tokenEntity);
                sentAtLeastOne = true;

            } catch (Exception e) {
                System.err.println("Failed to send test notification to token id "
                        + tokenEntity.getId() + ": " + e.getMessage());
            }
        }

        if (!sentAtLeastOne) {
            throw new BadRequestException("Failed to send test notification to all active tokens");
        }
    }
}