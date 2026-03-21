package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.EmployeeDeviceToken;
import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.repository.EmployeeDeviceFCMTokenRepository;
import edu.ijse.inshiftbackend.service.PresenceNotificationService;
import edu.ijse.inshiftbackend.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PresenceNotificationServiceImpl implements PresenceNotificationService {

    private final EmployeeDeviceFCMTokenRepository employeeDeviceFCMTokenRepository;
    private final PushNotificationService pushNotificationService;

    @Override
    public void sendPresenceCheckNotification(PresenceCheck presenceCheck) {
        List<EmployeeDeviceToken> activeTokens =
                employeeDeviceFCMTokenRepository.findAllByEmployeeAndActiveTrue(
                        presenceCheck.getEmployee()
                );

        if (activeTokens == null || activeTokens.isEmpty()) {
            return;
        }

        String title = "Presence verification required";
        String body = "Please confirm your presence now.";

        Map<String, String> data = new HashMap<>();
        data.put("type", "PRESENCE_CHECK");
        data.put("presenceCheckId", String.valueOf(presenceCheck.getId()));
        data.put("url", "/emp/verify?presenceCheckId=" + presenceCheck.getId());

        for (EmployeeDeviceToken tokenEntity : activeTokens) {
            try {
                pushNotificationService.sendToToken(
                        tokenEntity.getFcmToken(),
                        title,
                        body,
                        data
                );

                tokenEntity.setLastUsedAt(LocalDateTime.now());
                employeeDeviceFCMTokenRepository.save(tokenEntity);
            } catch (Exception e) {
                System.err.println("Failed to send push to token id "
                        + tokenEntity.getId() + ": " + e.getMessage());
            }
        }
    }
}