package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.AdminCreateOvertimeDTO;
import edu.ijse.inshiftbackend.dto.EmployeeDeclineOvertimeDTO;
import edu.ijse.inshiftbackend.dto.EmployeeOfferOvertimeSwapDTO;
import edu.ijse.inshiftbackend.dto.response.OvertimeAssignmentResponseDTO;
import edu.ijse.inshiftbackend.dto.response.OvertimeSwapResponseDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeDeviceToken;
import edu.ijse.inshiftbackend.entity.OvertimeAssignment;
import edu.ijse.inshiftbackend.entity.OvertimeSwapRequest;
import edu.ijse.inshiftbackend.entity.enums.OvertimeStatus;
import edu.ijse.inshiftbackend.entity.enums.OvertimeSwapStatus;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeDeviceFCMTokenRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.OvertimeAssignmentRepository;
import edu.ijse.inshiftbackend.repository.OvertimeSwapRequestRepository;
import edu.ijse.inshiftbackend.service.OvertimeService;
import edu.ijse.inshiftbackend.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OvertimeServiceImpl implements OvertimeService {

    private final EmployeeRepository employeeRepository;
    private final OvertimeAssignmentRepository overtimeAssignmentRepository;
    private final OvertimeSwapRequestRepository overtimeSwapRequestRepository;
    private final EmployeeDeviceFCMTokenRepository employeeDeviceFCMTokenRepository;
    private final PushNotificationService pushNotificationService;

    @Override
    @Transactional
    public OvertimeAssignmentResponseDTO adminCreateOvertime(AdminCreateOvertimeDTO dto, String adminEmail) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Employee admin = employeeRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        long totalMinutes = Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes() - dto.getBreakMinutes();
        if (totalMinutes <= 0) {
            throw new BadRequestException("Calculated OT duration must be greater than zero");
        }

        OvertimeAssignment assignment = OvertimeAssignment.builder()
                .employee(employee)
                .otDate(dto.getOtDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .breakMinutes(dto.getBreakMinutes())
                .durationMinutes((int) totalMinutes)
                .reason(dto.getReason())
                .status(OvertimeStatus.ASSIGNED)
                .employeeResponseNote(null)
                .assignedBy(admin)
                .assignedAt(LocalDateTime.now())
                .build();

        OvertimeAssignment saved = overtimeAssignmentRepository.save(assignment);

        log.info(
                "OT assigned successfully. overtimeId={}, employeeId={}, assignedBy={}, otDate={}",
                saved.getId(),
                employee.getEmployeeId(),
                admin.getEmployeeId(),
                saved.getOtDate()
        );

        Map<String, String> data = new HashMap<>();
        data.put("type", "OT_ASSIGNED");
        data.put("overtimeId", String.valueOf(saved.getId()));
        data.put("url", "/emp/overtime");

        notifyEmployee(
                employee,
                "Overtime Assigned",
                "You have been assigned overtime on " + saved.getOtDate(),
                data
        );

        return mapToAssignmentDTO(saved);
    }

    private void notifyEmployee(Employee employee, String title, String body, Map<String, String> data) {
        List<EmployeeDeviceToken> tokens =
                employeeDeviceFCMTokenRepository.findAllByEmployeeAndActiveTrue(employee);

        if (tokens == null || tokens.isEmpty()) {
            log.info("No active OT notification tokens found for employee {}", employee.getEmployeeId());
            return;
        }

        for (EmployeeDeviceToken token : tokens) {
            if (token.getFcmToken() == null || token.getFcmToken().isBlank()) {
                log.warn("Skipping OT notification because token is blank for token id {}", token.getId());
                continue;
            }

            try {
                pushNotificationService.sendToToken(
                        token.getFcmToken(),
                        title,
                        body,
                        data
                );

                token.setLastUsedAt(LocalDateTime.now());
                employeeDeviceFCMTokenRepository.save(token);

                log.info(
                        "OT notification sent successfully to token id {} for employee {}",
                        token.getId(),
                        employee.getEmployeeId()
                );

            } catch (Exception e) {
                log.warn("Failed OT notification to token id {}", token.getId(), e);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OvertimeAssignmentResponseDTO> getAllOvertimeAssignments() {
        return overtimeAssignmentRepository.findAllByOrderByAssignedAtDesc()
                .stream()
                .map(this::mapToAssignmentDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OvertimeAssignmentResponseDTO> getMyOvertimeAssignments(String employeeEmail) {
        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        return overtimeAssignmentRepository.findByEmployeeOrderByAssignedAtDesc(employee)
                .stream()
                .map(this::mapToAssignmentDTO)
                .toList();
    }

    @Override
    @Transactional
    public OvertimeAssignmentResponseDTO acceptOvertime(Long overtimeId, String employeeEmail) {
        OvertimeAssignment assignment = getOwnedAssignment(overtimeId, employeeEmail);

        if (assignment.getStatus() != OvertimeStatus.ASSIGNED) {
            throw new BadRequestException("Only assigned OT can be accepted");
        }

        if (isOtOver(assignment)) {
            throw new BadRequestException("This overtime assignment is already over");
        }

        assignment.setStatus(OvertimeStatus.ACCEPTED);
        assignment.setEmployeeResponseNote(null);

        OvertimeAssignment saved = overtimeAssignmentRepository.save(assignment);

        log.info(
                "OT accepted. overtimeId={}, employeeId={}",
                saved.getId(),
                saved.getEmployee().getEmployeeId()
        );

        Map<String, String> data = new HashMap<>();
        data.put("type", "OT_ACCEPTED");
        data.put("overtimeId", String.valueOf(saved.getId()));
        data.put("url", "/admin/overtime");

        notifyEmployee(
                saved.getAssignedBy(),
                "Overtime Accepted",
                saved.getEmployee().getFullName() + " accepted the overtime",
                data
        );

        return mapToAssignmentDTO(saved);
    }

    @Override
    @Transactional
    public OvertimeAssignmentResponseDTO declineOvertime(Long overtimeId, EmployeeDeclineOvertimeDTO dto, String employeeEmail) {
        OvertimeAssignment assignment = getOwnedAssignment(overtimeId, employeeEmail);

        if (assignment.getStatus() != OvertimeStatus.ASSIGNED) {
            throw new BadRequestException("Only assigned OT can be declined");
        }

        if (isOtOver(assignment)) {
            throw new BadRequestException("This overtime assignment is already over");
        }

        assignment.setStatus(OvertimeStatus.DECLINED);
        assignment.setEmployeeResponseNote(dto.getNote());

        OvertimeAssignment saved = overtimeAssignmentRepository.save(assignment);

        log.info(
                "OT declined. overtimeId={}, employeeId={}",
                saved.getId(),
                saved.getEmployee().getEmployeeId()
        );

        Map<String, String> data = new HashMap<>();
        data.put("type", "OT_DECLINED");
        data.put("overtimeId", String.valueOf(saved.getId()));
        data.put("url", "/admin/overtime");

        notifyEmployee(
                saved.getAssignedBy(),
                "Overtime Declined",
                saved.getEmployee().getFullName() + " declined OT",
                data
        );

        return mapToAssignmentDTO(saved);
    }

    @Override
    @Transactional
    public OvertimeSwapResponseDTO offerSwap(Long overtimeId, EmployeeOfferOvertimeSwapDTO dto, String employeeEmail) {
        OvertimeAssignment assignment = getOwnedAssignment(overtimeId, employeeEmail);

        if (assignment.getStatus() != OvertimeStatus.ACCEPTED) {
            throw new BadRequestException("Only accepted OT can be swapped");
        }

        if (isOtStarted(assignment)) {
            throw new BadRequestException("Swap is not allowed because this OT has already started or ended");
        }

        Employee fromEmployee = assignment.getEmployee();

        Employee toEmployee = employeeRepository.findById(dto.getToEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Target employee not found"));

        if (fromEmployee.getEmployeeId().equals(toEmployee.getEmployeeId())) {
            throw new BadRequestException("You cannot offer a swap to yourself");
        }

        boolean alreadyPending = overtimeSwapRequestRepository
                .findTopByOvertimeAssignmentAndStatusOrderByCreatedAtDesc(assignment, OvertimeSwapStatus.PENDING)
                .isPresent();

        if (alreadyPending) {
            throw new BadRequestException("A swap request is already pending for this OT");
        }

        OvertimeSwapRequest swapRequest = OvertimeSwapRequest.builder()
                .overtimeAssignment(assignment)
                .fromEmployee(fromEmployee)
                .toEmployee(toEmployee)
                .note(dto.getNote())
                .status(OvertimeSwapStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        assignment.setStatus(OvertimeStatus.SWAP_PENDING);
        overtimeAssignmentRepository.save(assignment);

        OvertimeSwapRequest savedSwap = overtimeSwapRequestRepository.save(swapRequest);

        log.info(
                "OT swap offered. swapRequestId={}, overtimeId={}, fromEmployeeId={}, toEmployeeId={}",
                savedSwap.getId(),
                assignment.getId(),
                fromEmployee.getEmployeeId(),
                toEmployee.getEmployeeId()
        );

        Map<String, String> data = new HashMap<>();
        data.put("type", "OT_SWAP_REQUEST");
        data.put("swapRequestId", String.valueOf(savedSwap.getId()));
        data.put("url", "/emp/overtime/swaps");

        notifyEmployee(
                toEmployee,
                "Overtime Swap Request",
                fromEmployee.getFullName() + " wants to swap OT with you",
                data
        );

        return mapToSwapDTO(savedSwap);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OvertimeSwapResponseDTO> getIncomingSwapRequests(String employeeEmail) {
        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        return overtimeSwapRequestRepository
                .findByToEmployeeAndStatusOrderByCreatedAtDesc(employee, OvertimeSwapStatus.PENDING)
                .stream()
                .map(this::mapToSwapDTO)
                .toList();
    }

    @Override
    @Transactional
    public OvertimeSwapResponseDTO acceptSwap(Long swapRequestId, String employeeEmail) {
        OvertimeSwapRequest swapRequest = getOwnedIncomingSwapRequest(swapRequestId, employeeEmail);

        if (swapRequest.getStatus() != OvertimeSwapStatus.PENDING) {
            throw new BadRequestException("Only pending swap requests can be accepted");
        }

        OvertimeAssignment assignment = swapRequest.getOvertimeAssignment();

        if (isOtStarted(assignment)) {
            throw new BadRequestException("This OT can no longer be swapped because it has already started or ended");
        }

        assignment.setEmployee(swapRequest.getToEmployee());
        assignment.setStatus(OvertimeStatus.ACCEPTED);
        assignment.setEmployeeResponseNote(null);

        swapRequest.setStatus(OvertimeSwapStatus.ACCEPTED);
        swapRequest.setRespondedAt(LocalDateTime.now());

        overtimeAssignmentRepository.save(assignment);
        overtimeSwapRequestRepository.save(swapRequest);

        log.info(
                "OT swap accepted. swapRequestId={}, overtimeId={}, newEmployeeId={}",
                swapRequest.getId(),
                assignment.getId(),
                swapRequest.getToEmployee().getEmployeeId()
        );

        Map<String, String> data = new HashMap<>();
        data.put("type", "OT_SWAP_ACCEPTED");
        data.put("overtimeId", String.valueOf(assignment.getId()));
        data.put("url", "/emp/overtime");

        notifyEmployee(
                swapRequest.getFromEmployee(),
                "Swap Accepted",
                swapRequest.getToEmployee().getFullName() + " accepted your swap",
                data
        );

        return mapToSwapDTO(swapRequest);
    }

    @Override
    @Transactional
    public OvertimeSwapResponseDTO rejectSwap(Long swapRequestId, String employeeEmail) {
        OvertimeSwapRequest swapRequest = getOwnedIncomingSwapRequest(swapRequestId, employeeEmail);

        if (swapRequest.getStatus() != OvertimeSwapStatus.PENDING) {
            throw new BadRequestException("Only pending swap requests can be rejected");
        }

        OvertimeAssignment assignment = swapRequest.getOvertimeAssignment();

        if (isOtStarted(assignment)) {
            throw new BadRequestException("This OT can no longer be updated because it has already started or ended");
        }

        swapRequest.setStatus(OvertimeSwapStatus.REJECTED);
        swapRequest.setRespondedAt(LocalDateTime.now());

        assignment.setStatus(OvertimeStatus.ACCEPTED);

        overtimeAssignmentRepository.save(assignment);
        overtimeSwapRequestRepository.save(swapRequest);

        log.info(
                "OT swap rejected. swapRequestId={}, overtimeId={}, originalEmployeeId={}",
                swapRequest.getId(),
                assignment.getId(),
                swapRequest.getFromEmployee().getEmployeeId()
        );

        Map<String, String> data = new HashMap<>();
        data.put("type", "OT_SWAP_REJECTED");
        data.put("overtimeId", String.valueOf(assignment.getId()));
        data.put("url", "/emp/overtime");

        notifyEmployee(
                swapRequest.getFromEmployee(),
                "Swap Rejected",
                swapRequest.getToEmployee().getFullName() + " rejected your swap",
                data
        );

        return mapToSwapDTO(swapRequest);
    }

    private OvertimeAssignment getOwnedAssignment(Long overtimeId, String employeeEmail) {
        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        OvertimeAssignment assignment = overtimeAssignmentRepository.findById(overtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("OT assignment not found"));

        if (!assignment.getEmployee().getEmployeeId().equals(employee.getEmployeeId())) {
            throw new BadRequestException("You do not own this OT assignment");
        }

        return assignment;
    }

    private OvertimeSwapRequest getOwnedIncomingSwapRequest(Long swapRequestId, String employeeEmail) {
        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        OvertimeSwapRequest swapRequest = overtimeSwapRequestRepository.findById(swapRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("OT swap request not found"));

        if (!swapRequest.getToEmployee().getEmployeeId().equals(employee.getEmployeeId())) {
            throw new BadRequestException("You do not own this incoming swap request");
        }

        return swapRequest;
    }

    private LocalDateTime getOtStartDateTime(OvertimeAssignment assignment) {
        return LocalDateTime.of(assignment.getOtDate(), assignment.getStartTime());
    }

    private LocalDateTime getOtEndDateTime(OvertimeAssignment assignment) {
        return LocalDateTime.of(assignment.getOtDate(), assignment.getEndTime());
    }

    private boolean isOtStarted(OvertimeAssignment assignment) {
        return !LocalDateTime.now().isBefore(getOtStartDateTime(assignment));
    }

    private boolean isOtOver(OvertimeAssignment assignment) {
        return LocalDateTime.now().isAfter(getOtEndDateTime(assignment));
    }

    private String resolveDisplayStatus(OvertimeAssignment assignment) {
        if (isOtOver(assignment)) {
            return "OT_OVER";
        }

        if (isOtStarted(assignment)) {
            return "IN_PROGRESS";
        }

        return assignment.getStatus() != null ? assignment.getStatus().name() : "UNKNOWN";
    }

    private OvertimeAssignmentResponseDTO mapToAssignmentDTO(OvertimeAssignment assignment) {
        boolean otStarted = isOtStarted(assignment);
        boolean otOver = isOtOver(assignment);

        boolean canAccept = assignment.getStatus() == OvertimeStatus.ASSIGNED && !otOver;
        boolean canDecline = assignment.getStatus() == OvertimeStatus.ASSIGNED && !otOver;
        boolean canSwap = assignment.getStatus() == OvertimeStatus.ACCEPTED && !otStarted;

        return OvertimeAssignmentResponseDTO.builder()
                .id(assignment.getId())
                .employeeId(assignment.getEmployee().getEmployeeId())
                .employeeName(assignment.getEmployee().getFullName())
                .employeeCode(assignment.getEmployee().getEmpCode())
                .otDate(assignment.getOtDate())
                .startTime(assignment.getStartTime())
                .endTime(assignment.getEndTime())
                .breakMinutes(assignment.getBreakMinutes())
                .durationMinutes(assignment.getDurationMinutes())
                .reason(assignment.getReason())
                .status(assignment.getStatus().name())
                .employeeResponseNote(assignment.getEmployeeResponseNote())
                .assignedById(assignment.getAssignedBy().getEmployeeId())
                .assignedByName(assignment.getAssignedBy().getFullName())
                .assignedAt(assignment.getAssignedAt())
                .updatedAt(assignment.getUpdatedAt())
                .otStarted(otStarted)
                .otOver(otOver)
                .canAccept(canAccept)
                .canDecline(canDecline)
                .canSwap(canSwap)
                .displayStatus(resolveDisplayStatus(assignment))
                .build();
    }

    private OvertimeSwapResponseDTO mapToSwapDTO(OvertimeSwapRequest swapRequest) {
        return OvertimeSwapResponseDTO.builder()
                .id(swapRequest.getId())
                .overtimeAssignmentId(swapRequest.getOvertimeAssignment().getId())
                .fromEmployeeId(swapRequest.getFromEmployee().getEmployeeId())
                .fromEmployeeName(swapRequest.getFromEmployee().getFullName())
                .toEmployeeId(swapRequest.getToEmployee().getEmployeeId())
                .toEmployeeName(swapRequest.getToEmployee().getFullName())
                .note(swapRequest.getNote())
                .status(swapRequest.getStatus().name())
                .createdAt(swapRequest.getCreatedAt())
                .respondedAt(swapRequest.getRespondedAt())
                .build();
    }
}