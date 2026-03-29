package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.EmployeeDeviceToken;
import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.enums.DeviceType;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckSourceExpected;
import edu.ijse.inshiftbackend.repository.EmployeeDeviceFCMTokenRepository;
import edu.ijse.inshiftbackend.service.PresenceNotificationService;
import edu.ijse.inshiftbackend.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceNotificationServiceImpl implements PresenceNotificationService {

    private final EmployeeDeviceFCMTokenRepository employeeDeviceFCMTokenRepository;
    private final PushNotificationService pushNotificationService;

    @Override
    public void sendPresenceCheckNotification(PresenceCheck presenceCheck) {
        if (presenceCheck == null || presenceCheck.getEmployee() == null) {
            log.warn("Skipping presence notification because presenceCheck or employee is null");
            return;
        }

        List<EmployeeDeviceToken> activeTokens =
                employeeDeviceFCMTokenRepository.findAllByEmployeeAndActiveTrue(
                        presenceCheck.getEmployee()
                );

        if (activeTokens == null || activeTokens.isEmpty()) {
            log.info(
                    "No active device tokens found for employee {} when sending presence check {}",
                    presenceCheck.getEmployee().getEmployeeId(),
                    presenceCheck.getId()
            );
            return;
        }

        String title = "Presence verification required";
        String body = buildNotificationBody(presenceCheck);
        Map<String, String> data = buildNotificationData(presenceCheck);

        for (EmployeeDeviceToken tokenEntity : activeTokens) {
            if (tokenEntity == null || tokenEntity.getFcmToken() == null || tokenEntity.getFcmToken().isBlank()) {
                log.warn("Skipping presence notification because token entity or FCM token is invalid");
                continue;
            }

            if (!isRelevantTargetToken(tokenEntity, presenceCheck)) {
                log.info(
                        "Skipping token {} because device type {} is not relevant for presence check {} expected source {}",
                        tokenEntity.getId(),
                        tokenEntity.getDeviceType(),
                        presenceCheck.getId(),
                        presenceCheck.getSourceExpected()
                );
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

                log.info(
                        "Presence notification sent successfully for presence check {} to token id {}",
                        presenceCheck.getId(),
                        tokenEntity.getId()
                );

            } catch (Exception e) {
                log.warn(
                        "Failed to send presence notification for presence check {} to device token id {}",
                        presenceCheck.getId(),
                        tokenEntity.getId(),
                        e
                );
            }
        }
    }

    private boolean isRelevantTargetToken(EmployeeDeviceToken tokenEntity, PresenceCheck presenceCheck) {
        if (presenceCheck.getSourceExpected() == null || presenceCheck.getSourceExpected() == PresenceCheckSourceExpected.ANY) {
            return true;
        }

        if (presenceCheck.getSourceExpected() == PresenceCheckSourceExpected.COMPANY_PC) {
            return tokenEntity.getDeviceType() == DeviceType.COMPANY_PC;
        }

        if (presenceCheck.getSourceExpected() == PresenceCheckSourceExpected.MOBILE_BIOMETRIC) {
            return tokenEntity.getDeviceType() == DeviceType.MOBILE;
        }

        return true;
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
        data.put(
                "sourceExpected",
                presenceCheck.getSourceExpected() != null
                        ? presenceCheck.getSourceExpected().name()
                        : "ANY"
        );
        data.put("url", "/emp/presence-check?presenceCheckId=" + presenceCheck.getId());
        return data;
    }
}





//package edu.ijse.inshiftbackend.service.impl;
//
//import edu.ijse.inshiftbackend.entity.EmployeeDeviceToken;
//import edu.ijse.inshiftbackend.entity.PresenceCheck;
//import edu.ijse.inshiftbackend.entity.enums.DeviceType;
//import edu.ijse.inshiftbackend.entity.enums.PresenceCheckSourceExpected;
//import edu.ijse.inshiftbackend.repository.EmployeeDeviceFCMTokenRepository;
//import edu.ijse.inshiftbackend.service.PresenceNotificationService;
//import edu.ijse.inshiftbackend.service.PushNotificationService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class PresenceNotificationServiceImpl implements PresenceNotificationService {
//
//    private final EmployeeDeviceFCMTokenRepository employeeDeviceFCMTokenRepository;
//    private final PushNotificationService pushNotificationService;
//
//    @Override
//    public void sendPresenceCheckNotification(PresenceCheck presenceCheck) {
//        if (presenceCheck == null || presenceCheck.getEmployee() == null) {
//            return;
//        }
//
//        List<EmployeeDeviceToken> activeTokens =
//                employeeDeviceFCMTokenRepository.findAllByEmployeeAndActiveTrue(
//                        presenceCheck.getEmployee()
//                );
//
//        if (activeTokens == null || activeTokens.isEmpty()) {
//            return;
//        }
//
//        String title = "Presence verification required";
//        String body = buildNotificationBody(presenceCheck);
//        Map<String, String> data = buildNotificationData(presenceCheck);
//
//        for (EmployeeDeviceToken tokenEntity : activeTokens) {
//            if (tokenEntity == null || tokenEntity.getFcmToken() == null || tokenEntity.getFcmToken().isBlank()) {
//                continue;
//            }
//
//            if (!isRelevantTargetToken(tokenEntity, presenceCheck)) {
//                continue;
//            }
//
//            try {
//                pushNotificationService.sendToToken(
//                        tokenEntity.getFcmToken(),
//                        title,
//                        body,
//                        data
//                );
//
//                tokenEntity.setLastUsedAt(LocalDateTime.now());
//                employeeDeviceFCMTokenRepository.save(tokenEntity);
//
//            } catch (Exception e) {
//                System.err.println(
//                        "Failed to send presence notification to device token id "
//                                + tokenEntity.getId() + ": " + e.getMessage()
//                );
//            }
//        }
//    }
//
//    private boolean isRelevantTargetToken(EmployeeDeviceToken tokenEntity, PresenceCheck presenceCheck) {
//        if (presenceCheck.getSourceExpected() == null || presenceCheck.getSourceExpected() == PresenceCheckSourceExpected.ANY) {
//            return true;
//        }
//
//        if (presenceCheck.getSourceExpected() == PresenceCheckSourceExpected.COMPANY_PC) {
//            return tokenEntity.getDeviceType() == DeviceType.COMPANY_PC;
//        }
//
//        if (presenceCheck.getSourceExpected() == PresenceCheckSourceExpected.MOBILE_BIOMETRIC) {
//            return tokenEntity.getDeviceType() == DeviceType.MOBILE;
//        }
//
//        return true;
//    }
//
//    private String buildNotificationBody(PresenceCheck presenceCheck) {
//        if (presenceCheck.getSourceExpected() == null) {
//            return "Please confirm your presence now.";
//        }
//
//        return switch (presenceCheck.getSourceExpected()) {
//            case COMPANY_PC -> "Presence check available. Please confirm from your approved company PC.";
//            case MOBILE_BIOMETRIC -> "Presence check available. Please confirm from your approved mobile device.";
//            case ANY -> "Please confirm your presence now.";
//        };
//    }
//
//    private Map<String, String> buildNotificationData(PresenceCheck presenceCheck) {
//        Map<String, String> data = new HashMap<>();
//        data.put("type", "PRESENCE_CHECK");
//        data.put("presenceCheckId", String.valueOf(presenceCheck.getId()));
//        data.put(
//                "sourceExpected",
//                presenceCheck.getSourceExpected() != null
//                        ? presenceCheck.getSourceExpected().name()
//                        : "ANY"
//        );
//        data.put("url", "/emp/presence-check?presenceCheckId=" + presenceCheck.getId());
//        return data;
//    }
//}