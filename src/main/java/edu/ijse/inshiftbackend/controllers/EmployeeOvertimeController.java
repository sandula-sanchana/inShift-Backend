package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.EmployeeDeclineOvertimeDTO;
import edu.ijse.inshiftbackend.dto.EmployeeOfferOvertimeSwapDTO;
import edu.ijse.inshiftbackend.dto.response.OvertimeAssignmentResponseDTO;
import edu.ijse.inshiftbackend.dto.response.OvertimeSwapResponseDTO;
import edu.ijse.inshiftbackend.service.OvertimeService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/emp/ot")
@RequiredArgsConstructor
@CrossOrigin
public class EmployeeOvertimeController {

    private final OvertimeService overtimeService;

    @GetMapping("/my")
    public APIResponse<List<OvertimeAssignmentResponseDTO>> myAssignments(Authentication auth) {
        return new APIResponse<>(
                200,
                "My OT assignments fetched successfully",
                overtimeService.getMyOvertimeAssignments(auth.getName())
        );
    }

    @PatchMapping("/{overtimeId}/accept")
    public APIResponse<OvertimeAssignmentResponseDTO> accept(
            @PathVariable Long overtimeId,
            Authentication auth
    ) {
        return new APIResponse<>(
                200,
                "OT accepted successfully",
                overtimeService.acceptOvertime(overtimeId, auth.getName())
        );
    }

    @PatchMapping("/{overtimeId}/decline")
    public APIResponse<OvertimeAssignmentResponseDTO> decline(
            @PathVariable Long overtimeId,
            @RequestBody @Valid EmployeeDeclineOvertimeDTO dto,
            Authentication auth
    ) {
        return new APIResponse<>(
                200,
                "OT declined successfully",
                overtimeService.declineOvertime(overtimeId, dto, auth.getName())
        );
    }

    @PostMapping("/{overtimeId}/swap-offer")
    public APIResponse<OvertimeSwapResponseDTO> offerSwap(
            @PathVariable Long overtimeId,
            @RequestBody @Valid EmployeeOfferOvertimeSwapDTO dto,
            Authentication auth
    ) {
        return new APIResponse<>(
                200,
                "OT swap offered successfully",
                overtimeService.offerSwap(overtimeId, dto, auth.getName())
        );
    }

    @GetMapping("/swaps/incoming")
    public APIResponse<List<OvertimeSwapResponseDTO>> incomingSwaps(Authentication auth) {
        return new APIResponse<>(
                200,
                "Incoming OT swap requests fetched successfully",
                overtimeService.getIncomingSwapRequests(auth.getName())
        );
    }

    @PatchMapping("/swaps/{swapRequestId}/accept")
    public APIResponse<OvertimeSwapResponseDTO> acceptSwap(
            @PathVariable Long swapRequestId,
            Authentication auth
    ) {
        return new APIResponse<>(
                200,
                "OT swap accepted successfully",
                overtimeService.acceptSwap(swapRequestId, auth.getName())
        );
    }

    @PatchMapping("/swaps/{swapRequestId}/reject")
    public APIResponse<OvertimeSwapResponseDTO> rejectSwap(
            @PathVariable Long swapRequestId,
            Authentication auth
    ) {
        return new APIResponse<>(
                200,
                "OT swap rejected successfully",
                overtimeService.rejectSwap(swapRequestId, auth.getName())
        );
    }
}