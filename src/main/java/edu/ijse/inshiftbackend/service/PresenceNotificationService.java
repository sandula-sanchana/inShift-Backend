package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.entity.PresenceCheck;

public interface PresenceNotificationService {
    void sendPresenceCheckNotification(PresenceCheck presenceCheck);
}