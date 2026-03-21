package edu.ijse.inshiftbackend.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import edu.ijse.inshiftbackend.service.PushNotificationService;
import org.springframework.stereotype.Service;

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
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }

            FirebaseMessaging.getInstance().send(builder.build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send push notification", e);
        }
    }
}