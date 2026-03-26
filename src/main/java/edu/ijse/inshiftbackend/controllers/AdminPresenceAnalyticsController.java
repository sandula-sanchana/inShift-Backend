package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.response.PresenceAnalyticsResponseDTO;
import edu.ijse.inshiftbackend.service.PresenceAnalyticsService;
import edu.ijse.inshiftbackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/presence-analytics")
@CrossOrigin
public class AdminPresenceAnalyticsController {

    private final PresenceAnalyticsService presenceAnalyticsService;

    @GetMapping("/employee/{employeeId}")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<PresenceAnalyticsResponseDTO> getEmployeePresenceAnalytics(
            @PathVariable Long employeeId
    ) {
        return new APIResponse<>(
                200,
                "Presence analytics fetched successfully",
                presenceAnalyticsService.getEmployeePresenceAnalytics(employeeId)
        );
    }
}