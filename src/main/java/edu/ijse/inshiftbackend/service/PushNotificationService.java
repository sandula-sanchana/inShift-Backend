package edu.ijse.inshiftbackend.service;

import java.util.Map;

public interface PushNotificationService {

    void sendToToken(
            String token,
            String title,
            String body,
            Map<String, String> data
    );
}