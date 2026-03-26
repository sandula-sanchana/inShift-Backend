package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.AdminDeviceRevokeRequestDTO;
import edu.ijse.inshiftbackend.dto.response.AdminEmployeeDeviceResponseDTO;
import edu.ijse.inshiftbackend.service.AdminDeviceManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/devices")
@RequiredArgsConstructor
public class AdminDeviceManagementController {

    private final AdminDeviceManagementService adminDeviceManagementService;

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Map<String, Object>> getEmployeeDevices(@PathVariable Long employeeId) {
        List<AdminEmployeeDeviceResponseDTO> data =
                adminDeviceManagementService.getEmployeeDevices(employeeId);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Employee devices fetched successfully",
                "data", data
        ));
    }

    @PatchMapping("/{deviceId}/revoke")
    public ResponseEntity<Map<String, Object>> revokeDevice(
            @PathVariable Long deviceId,
            @RequestBody(required = false) AdminDeviceRevokeRequestDTO request
    ) {
        String reason = request != null ? request.getReason() : null;
        adminDeviceManagementService.revokeDevice(deviceId, reason);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Device revoked successfully"
        ));
    }

    @PatchMapping("/{deviceId}/restore")
    public ResponseEntity<Map<String, Object>> restoreDevice(
            @PathVariable Long deviceId,
            @RequestBody(required = false) AdminDeviceRevokeRequestDTO request
    ) {
        String reason = request != null ? request.getReason() : null;
        adminDeviceManagementService.restoreDevice(deviceId, reason);

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Device restored successfully"
        ));
    }
}