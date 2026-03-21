package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.DeviceEnrollmentDecisionDTO;
import edu.ijse.inshiftbackend.dto.response.AdminDeviceEnrollmentRequestResponseDTO;
import edu.ijse.inshiftbackend.service.DeviceEnrollmentRequestService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/device-enrollment")
@CrossOrigin
public class AdminDeviceEnrollmentController {

    private final DeviceEnrollmentRequestService enrollmentRequestService;

    @GetMapping("/pending")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<List<AdminDeviceEnrollmentRequestResponseDTO>> getPendingRequests() {
        return new APIResponse<>(
                200,
                "Pending enrollment requests fetched successfully",
                enrollmentRequestService.getPendingRequests()
        );
    }

    @PatchMapping("/{id}/decision")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<Void> decide(
            @PathVariable Long id,
            @Valid @RequestBody DeviceEnrollmentDecisionDTO dto
    ) {
        if (Boolean.TRUE.equals(dto.getApprove())) {
            enrollmentRequestService.approveRequest(id, dto.getAdminComment());
            return new APIResponse<>(
                    200,
                    "Enrollment request approved",
                    null
            );
        }

        enrollmentRequestService.rejectRequest(id, dto.getAdminComment());
        return new APIResponse<>(
                200,
                "Enrollment request rejected",
                null
        );
    }
}