package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.EmployeeBehaviorScore;
import edu.ijse.inshiftbackend.entity.PresenceCheckPlan;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckPlanStatus;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckRiskLevel;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckSourceExpected;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckTriggerReason;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.EmployeeBehaviorScoreRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.PresenceCheckPlanRepository;
import edu.ijse.inshiftbackend.service.PresenceCheckPlanningService;
import edu.ijse.inshiftbackend.service.TrustedDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PresenceCheckPlanningServiceImpl implements PresenceCheckPlanningService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeBehaviorScoreRepository employeeBehaviorScoreRepository;
    private final PresenceCheckPlanRepository presenceCheckPlanRepository;
    private final TrustedDeviceService trustedDeviceService;

    private final Random random;

    @Override
    @Transactional
    public List<PresenceCheckPlan> generateDailyPlansForEmployee(Long employeeId, LocalDate attendanceDate) {
        if (employeeId == null) {
            throw new BadRequestException("Employee id is required");
        }

        if (attendanceDate == null) {
            throw new BadRequestException("Attendance date is required");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        boolean alreadyPlanned = presenceCheckPlanRepository.existsByEmployeeEmployeeIdAndAttendanceDateAndStatus(
                employeeId,
                attendanceDate,
                PresenceCheckPlanStatus.PLANNED
        );

        if (alreadyPlanned) {
            return presenceCheckPlanRepository.findByEmployeeEmployeeIdAndAttendanceDateOrderByPlannedAtAsc(
                    employeeId,
                    attendanceDate
            );
        }

        EmployeeBehaviorScore behaviorScore = employeeBehaviorScoreRepository
                .findByEmployeeEmployeeId(employeeId)
                .orElse(
                        EmployeeBehaviorScore.builder()
                                .employee(employee)
                                .currentRiskScore(0)
                                .currentTrustScore(100)
                                .build()
                );

        int currentRiskScore = behaviorScore.getCurrentRiskScore() != null
                ? behaviorScore.getCurrentRiskScore()
                : 0;

        int planCount = resolvePlanCount(currentRiskScore);
        PresenceCheckRiskLevel riskLevel = resolveRiskLevel(currentRiskScore);
        int dueInMinutes = resolveDueWindowMinutes(currentRiskScore);
        PresenceCheckSourceExpected expectedSource = resolveExpectedSource(employee);

        List<LocalDateTime> randomTimes = generateRandomTimesForDay(attendanceDate, planCount);
        List<PresenceCheckPlan> plans = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < randomTimes.size(); i++) {
            PresenceCheckPlan plan = PresenceCheckPlan.builder()
                    .employee(employee)
                    .attendanceDate(attendanceDate)
                    .triggerReason(PresenceCheckTriggerReason.RANDOM)
                    .riskLevel(riskLevel)
                    .sourceExpected(expectedSource)
                    .status(PresenceCheckPlanStatus.PLANNED)
                    .description("Risk-adjusted random presence verification")
                    .plannedAt(randomTimes.get(i))
                    .triggeredAt(null)
                    .dueInMinutes(dueInMinutes)
                    .sequenceNo(i + 1)
                    .createdAt(now)
                    .build();

            plans.add(plan);
        }

        return presenceCheckPlanRepository.saveAll(plans);
    }

    @Override
    @Transactional
    public void generateDailyPlansForAllEligibleEmployees(LocalDate attendanceDate) {
        if (attendanceDate == null) {
            throw new BadRequestException("Attendance date is required");
        }

        List<Employee> employees = employeeRepository.findAllByActiveTrue();

        for (Employee employee : employees) {
            try {
                generateDailyPlansForEmployee(employee.getEmployeeId(), attendanceDate);
            } catch (Exception e) {
                System.err.println("Failed to generate presence-check plans for employee "
                        + employee.getEmployeeId() + ": " + e.getMessage());
            }
        }
    }

    private PresenceCheckSourceExpected resolveExpectedSource(Employee employee) {
        if (trustedDeviceService.hasApprovedCompanyPc(employee)) {
            return PresenceCheckSourceExpected.COMPANY_PC;
        }

        if (trustedDeviceService.hasApprovedMobile(employee)) {
            return PresenceCheckSourceExpected.MOBILE_BIOMETRIC;
        }

        return PresenceCheckSourceExpected.ANY;
    }

    private int resolvePlanCount(int currentRiskScore) {
        if (currentRiskScore >= 75) return 4;
        if (currentRiskScore >= 50) return 3;
        if (currentRiskScore >= 25) return 2;
        return 1;
    }

    private PresenceCheckRiskLevel resolveRiskLevel(int currentRiskScore) {
        if (currentRiskScore >= 60) return PresenceCheckRiskLevel.HIGH;
        if (currentRiskScore >= 25) return PresenceCheckRiskLevel.MEDIUM;
        return PresenceCheckRiskLevel.LOW;
    }

    private int resolveDueWindowMinutes(int currentRiskScore) {
        if (currentRiskScore >= 75) return 2;
        if (currentRiskScore >= 50) return 3;
        if (currentRiskScore >= 25) return 5;
        return 10;
    }

    private List<LocalDateTime> generateRandomTimesForDay(LocalDate date, int count) {
        LocalDateTime start = date.atTime(9, 0);
        LocalDateTime end = date.atTime(17, 0);

        long totalMinutes = Duration.between(start, end).toMinutes();
        if (totalMinutes <= 0) {
            throw new BadRequestException("Invalid planning window for presence checks");
        }

        List<LocalDateTime> result = new ArrayList<>();

        int safeCount = Math.max(1, count);

        while (result.size() < safeCount) {
            int randomMinute = random.nextInt((int) totalMinutes);
            LocalDateTime candidate = start.plusMinutes(randomMinute);

            if (!result.contains(candidate)) {
                result.add(candidate);
            }

            if (result.size() >= totalMinutes) {
                break;
            }
        }

        return result.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}