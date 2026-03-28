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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PresenceCheckPlanningServiceImpl implements PresenceCheckPlanningService {

    private static final Set<PresenceCheckPlanStatus> DAY_PLAN_EXISTING_STATUSES = Set.of(
            PresenceCheckPlanStatus.PLANNED,
            PresenceCheckPlanStatus.EXECUTING,
            PresenceCheckPlanStatus.TRIGGERED,
            PresenceCheckPlanStatus.SKIPPED
    );

    private static final int WORK_START_HOUR = 9;
    private static final int WORK_END_HOUR = 17;
    private static final int MIN_GAP_MINUTES = 45;

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

        boolean alreadyPlannedForDay =
                presenceCheckPlanRepository.existsByEmployeeEmployeeIdAndAttendanceDateAndStatusIn(
                        employeeId,
                        attendanceDate,
                        DAY_PLAN_EXISTING_STATUSES
                );

        if (alreadyPlannedForDay) {
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

        LocalDateTime now = LocalDateTime.now();
        List<PresenceCheckPlan> plans = new ArrayList<>();

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
                System.err.println("[PresencePlanDaily] Failed to generate plans for employee "
                        + employee.getEmployeeId() + ": " + e.getMessage());
            }
        }
    }

    private PresenceCheckSourceExpected resolveExpectedSource(Employee employee) {
        boolean hasCompanyPc = trustedDeviceService.hasApprovedCompanyPc(employee);
        boolean hasMobile = trustedDeviceService.hasApprovedMobile(employee);

        if (hasCompanyPc && hasMobile) {
            return PresenceCheckSourceExpected.ANY;
        }

        if (hasCompanyPc) {
            return PresenceCheckSourceExpected.COMPANY_PC;
        }

        if (hasMobile) {
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
        if (currentRiskScore >= 75) return 5;
        if (currentRiskScore >= 50) return 7;
        if (currentRiskScore >= 25) return 10;
        return 12;
    }

    private List<LocalDateTime> generateRandomTimesForDay(LocalDate date, int count) {
        LocalDateTime start = date.atTime(WORK_START_HOUR, 0);
        LocalDateTime end = date.atTime(WORK_END_HOUR, 0);

        long totalMinutes = Duration.between(start, end).toMinutes();
        if (totalMinutes <= 0) {
            throw new BadRequestException("Invalid planning window for presence checks");
        }

        int safeCount = Math.max(1, count);

        List<LocalDateTime> result = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = 500;

        while (result.size() < safeCount && attempts < maxAttempts) {
            attempts++;

            int randomMinute = random.nextInt((int) totalMinutes);
            LocalDateTime candidate = start.plusMinutes(randomMinute);

            boolean duplicate = result.contains(candidate);
            boolean tooClose = result.stream().anyMatch(existing ->
                    Math.abs(Duration.between(existing, candidate).toMinutes()) < MIN_GAP_MINUTES
            );

            if (!duplicate && !tooClose) {
                result.add(candidate);
            }
        }

        if (result.size() < safeCount) {
            result = generateEvenlySpacedTimes(start, end, safeCount);
        }

        return result.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private List<LocalDateTime> generateEvenlySpacedTimes(
            LocalDateTime start,
            LocalDateTime end,
            int count
    ) {
        List<LocalDateTime> times = new ArrayList<>();
        long totalMinutes = Duration.between(start, end).toMinutes();

        if (count <= 1) {
            times.add(start.plusMinutes(totalMinutes / 2));
            return times;
        }

        long step = totalMinutes / (count + 1);

        for (int i = 1; i <= count; i++) {
            times.add(start.plusMinutes(step * i));
        }

        return times;
    }
}