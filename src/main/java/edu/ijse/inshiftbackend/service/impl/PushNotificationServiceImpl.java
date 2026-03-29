package edu.ijse.inshiftbackend.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import edu.ijse.inshiftbackend.service.PushNotificationService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PushNotificationServiceImpl implements PushNotificationService {

    @Override
    public void sendToToken(
            String token,
            String title,
            String body,
            Map<String, String> data
    ) {
        try {
            Message.Builder builder = Message.builder()
                    .setToken(token);

            Map<String, String> payload = new HashMap<>();

            if (data != null && !data.isEmpty()) {
                payload.putAll(data);
            }

            payload.put("title", title != null && !title.isBlank() ? title : "InShift");
            payload.put("body", body != null && !body.isBlank() ? body : "You have a new notification.");

            builder.putAllData(payload);

            String id = FirebaseMessaging.getInstance().send(builder.build());
            System.out.println("FCM message sent id=" + id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send push notification", e);
        }
    }
}