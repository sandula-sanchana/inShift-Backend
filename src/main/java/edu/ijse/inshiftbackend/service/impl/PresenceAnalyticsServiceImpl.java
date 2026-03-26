package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.response.PresenceAnalyticsPointDTO;
import edu.ijse.inshiftbackend.dto.response.PresenceAnalyticsResponseDTO;
import edu.ijse.inshiftbackend.entity.AttendanceRiskScore;
import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.AttendanceRiskScoreRepository;
import edu.ijse.inshiftbackend.repository.EmployeeRepository;
import edu.ijse.inshiftbackend.repository.PresenceCheckRepository;
import edu.ijse.inshiftbackend.service.PresenceAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PresenceAnalyticsServiceImpl implements PresenceAnalyticsService {

    private final EmployeeRepository employeeRepository;
    private final PresenceCheckRepository presenceCheckRepository;
    private final AttendanceRiskScoreRepository attendanceRiskScoreRepository;

    @Override
    public PresenceAnalyticsResponseDTO getEmployeePresenceAnalytics(Long employeeId) {
        if (employeeId == null) {
            throw new BadRequestException("Employee id is required");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        List<PresenceCheck> allChecks =
                presenceCheckRepository.findByEmployeeEmployeeIdOrderByCreatedAtDesc(employeeId);

        int totalPresenceChecks = allChecks.size();

        int respondedCount = (int) allChecks.stream()
                .filter(pc -> pc.getStatus() != null &&
                        ("RESPONDED".equals(pc.getStatus().name()) || "LATE".equals(pc.getStatus().name())))
                .count();

        int lateCount = (int) allChecks.stream()
                .filter(pc -> Boolean.TRUE.equals(pc.getLateResponse()))
                .count();

        int missedCount = (int) allChecks.stream()
                .filter(pc -> Boolean.TRUE.equals(pc.getMissedResponse())
                        || (pc.getStatus() != null && "MISSED".equals(pc.getStatus().name())))
                .count();

        int escalatedCount = (int) allChecks.stream()
                .filter(pc -> Boolean.TRUE.equals(pc.getEscalated()))
                .count();

        List<Integer> delays = allChecks.stream()
                .map(PresenceCheck::getResponseDelaySeconds)
                .filter(Objects::nonNull)
                .toList();

        int averageDelay = delays.isEmpty()
                ? 0
                : (int) Math.round(delays.stream().mapToInt(Integer::intValue).average().orElse(0));

        int maxDelay = delays.stream().mapToInt(Integer::intValue).max().orElse(0);
        int minDelay = delays.stream().mapToInt(Integer::intValue).min().orElse(0);

        int companyPcResponses = (int) allChecks.stream()
                .filter(pc -> pc.getResponseSource() != null && "COMPANY_PC".equals(pc.getResponseSource().name()))
                .count();

        int mobileResponses = (int) allChecks.stream()
                .filter(pc -> pc.getResponseSource() != null && "MOBILE_GPS".equals(pc.getResponseSource().name()))
                .count();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);

        List<AttendanceRiskScore> riskScores =
                attendanceRiskScoreRepository.findByEmployeeEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        employeeId,
                        startDate,
                        endDate
                );

        List<String> riskTrendDates = riskScores.stream()
                .map(score -> score.getAttendanceDate().toString())
                .toList();

        List<Integer> riskTrendScores = riskScores.stream()
                .map(AttendanceRiskScore::getRiskScore)
                .toList();

        List<PresenceAnalyticsPointDTO> allPoints = allChecks.stream()
                .sorted(Comparator.comparing(PresenceCheck::getCreatedAt))
                .map(this::mapPoint)
                .toList();

        List<PresenceAnalyticsPointDTO> delayTrend = allPoints.stream()
                .filter(point -> point.getResponseDelaySeconds() != null)
                .toList();

        List<PresenceAnalyticsPointDTO> recentPresenceChecks = allPoints.stream()
                .sorted(Comparator.comparing(PresenceAnalyticsPointDTO::getCreatedAt).reversed())
                .limit(10)
                .toList();

        return PresenceAnalyticsResponseDTO.builder()
                .employeeId(employee.getEmployeeId())
                .employeeName(employee.getFullName())
                .totalPresenceChecks(totalPresenceChecks)
                .respondedCount(respondedCount)
                .lateCount(lateCount)
                .missedCount(missedCount)
                .escalatedCount(escalatedCount)
                .averageResponseDelaySeconds(averageDelay)
                .maxResponseDelaySeconds(maxDelay)
                .minResponseDelaySeconds(minDelay)
                .companyPcResponses(companyPcResponses)
                .mobileResponses(mobileResponses)
                .riskTrendDates(riskTrendDates)
                .riskTrendScores(riskTrendScores)
                .delayTrend(delayTrend)
                .recentPresenceChecks(recentPresenceChecks)
                .build();
    }

    private PresenceAnalyticsPointDTO mapPoint(PresenceCheck pc) {
        return PresenceAnalyticsPointDTO.builder()
                .presenceCheckId(pc.getId())
                .createdAt(pc.getCreatedAt())
                .respondedAt(pc.getRespondedAt())
                .status(pc.getStatus() != null ? pc.getStatus().name() : null)
                .triggerReason(pc.getTriggerReason() != null ? pc.getTriggerReason().name() : null)
                .riskLevel(pc.getRiskLevel() != null ? pc.getRiskLevel().name() : null)
                .responseSource(pc.getResponseSource() != null ? pc.getResponseSource().name() : null)
                .responseDelaySeconds(pc.getResponseDelaySeconds())
                .lateResponse(pc.getLateResponse())
                .missedResponse(pc.getMissedResponse())
                .escalated(pc.getEscalated())
                .build();
    }
}