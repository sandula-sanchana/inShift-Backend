package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.AdminCreateOvertimeDTO;
import edu.ijse.inshiftbackend.dto.EmployeeDeclineOvertimeDTO;
import edu.ijse.inshiftbackend.dto.EmployeeOfferOvertimeSwapDTO;
import edu.ijse.inshiftbackend.dto.response.OvertimeAssignmentResponseDTO;
import edu.ijse.inshiftbackend.dto.response.OvertimeSwapResponseDTO;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.OvertimeAssignment;
import edu.ijse.inshiftbackend.entity.OvertimeSwapRequest;
import edu.ijse.inshiftbackend.entity.enums.OvertimeStatus;
import edu.ijse.inshiftbackend.entity.enums.OvertimeSwapStatus;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.OvertimeAssignmentRepository;
import edu.ijse.inshiftbackend.repository.OvertimeSwapRequestRepository;
import edu.ijse.inshiftbackend.service.OvertimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OvertimeServiceImpl implements OvertimeService {

    private final EmployeeRepository employeeRepository;
    private final OvertimeAssignmentRepository overtimeAssignmentRepository;
    private final OvertimeSwapRequestRepository overtimeSwapRequestRepository;

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

        return mapToAssignmentDTO(overtimeAssignmentRepository.save(assignment));
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

        assignment.setStatus(OvertimeStatus.ACCEPTED);
        assignment.setEmployeeResponseNote(null);

        return mapToAssignmentDTO(overtimeAssignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public OvertimeAssignmentResponseDTO declineOvertime(Long overtimeId, EmployeeDeclineOvertimeDTO dto, String employeeEmail) {
        OvertimeAssignment assignment = getOwnedAssignment(overtimeId, employeeEmail);

        if (assignment.getStatus() != OvertimeStatus.ASSIGNED) {
            throw new BadRequestException("Only assigned OT can be declined");
        }

        assignment.setStatus(OvertimeStatus.DECLINED);
        assignment.setEmployeeResponseNote(dto.getNote());

        return mapToAssignmentDTO(overtimeAssignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public OvertimeSwapResponseDTO offerSwap(Long overtimeId, EmployeeOfferOvertimeSwapDTO dto, String employeeEmail) {
        OvertimeAssignment assignment = getOwnedAssignment(overtimeId, employeeEmail);

        if (assignment.getStatus() != OvertimeStatus.ASSIGNED && assignment.getStatus() != OvertimeStatus.ACCEPTED) {
            throw new BadRequestException("Only assigned or accepted OT can be swapped");
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

        return mapToSwapDTO(overtimeSwapRequestRepository.save(swapRequest));
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
        assignment.setEmployee(swapRequest.getToEmployee());
        assignment.setStatus(OvertimeStatus.ASSIGNED);
        assignment.setEmployeeResponseNote(null);

        swapRequest.setStatus(OvertimeSwapStatus.ACCEPTED);
        swapRequest.setRespondedAt(LocalDateTime.now());

        overtimeAssignmentRepository.save(assignment);
        overtimeSwapRequestRepository.save(swapRequest);

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
        assignment.setStatus(OvertimeStatus.ASSIGNED);

        swapRequest.setStatus(OvertimeSwapStatus.REJECTED);
        swapRequest.setRespondedAt(LocalDateTime.now());

        overtimeAssignmentRepository.save(assignment);
        overtimeSwapRequestRepository.save(swapRequest);

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

    private OvertimeAssignmentResponseDTO mapToAssignmentDTO(OvertimeAssignment assignment) {
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