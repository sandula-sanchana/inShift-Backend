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
        if (presenceCheck == null || presenceCheck.getEmployee() == null) {
            return;
        }

        List<EmployeeDeviceToken> activeTokens =
                employeeDeviceFCMTokenRepository.findAllByEmployeeAndActiveTrue(
                        presenceCheck.getEmployee()
                );

        if (activeTokens == null || activeTokens.isEmpty()) {
            return;
        }

        String title = "Presence verification required";
        String body = buildNotificationBody(presenceCheck);

        Map<String, String> data = buildNotificationData(presenceCheck);

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

            } catch (Exception e) {
                System.err.println(
                        "Failed to send presence notification to device token id "
                                + tokenEntity.getId() + ": " + e.getMessage()
                );
            }
        }
    }

    private String buildNotificationBody(PresenceCheck presenceCheck) {
        if (presenceCheck.getSourceExpected() == null) {
            return "Please confirm your presence now.";
        }

        return switch (presenceCheck.getSourceExpected()) {
            case COMPANY_PC -> "Presence check available. Please confirm from your approved company PC.";
            case MOBILE_BIOMETRIC -> "Presence check available. Please confirm from your approved mobile device.";
            case ANY -> "Please confirm your presence now.";
        };
    }

    private Map<String, String> buildNotificationData(PresenceCheck presenceCheck) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "PRESENCE_CHECK");
        data.put("presenceCheckId", String.valueOf(presenceCheck.getId()));
        data.put("sourceExpected",
                presenceCheck.getSourceExpected() != null
                        ? presenceCheck.getSourceExpected().name()
                        : "ANY");
        data.put("url", "/emp/presence-check");
        return data;
    }
}