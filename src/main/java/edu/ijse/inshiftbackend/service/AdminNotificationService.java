package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.AdminNotificationTestDTO;

public interface AdminNotificationService {
    void sendTestNotification(AdminNotificationTestDTO dto);
}