package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.AdminNotificationTestDTO;
import edu.ijse.inshiftbackend.service.AdminNotificationService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/notifications")
@CrossOrigin
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @PostMapping("/test")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<Void> sendTestNotification(
            @Valid @RequestBody AdminNotificationTestDTO dto
    ) {
        adminNotificationService.sendTestNotification(dto);

        return new APIResponse<>(
                200,
                "Test notification sent successfully",
                null
        );
    }
}