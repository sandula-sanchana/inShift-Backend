package edu.ijse.inshiftbackend.config;

import edu.ijse.inshiftbackend.entity.AttendanceRule;
import edu.ijse.inshiftbackend.entity.enums.AttendanceRuleKey;
import edu.ijse.inshiftbackend.entity.enums.RiskSeverity;
import edu.ijse.inshiftbackend.repository.AttendanceRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BootstrapAttendanceRuleInitializer implements CommandLineRunner {

    private final AttendanceRuleRepository attendanceRuleRepository;

    @Value("${inshift.bootstrap.rules.enabled:false}")
    private boolean enabled;

    @Override
    public void run(String... args) {

        if (!enabled) return;

        createIfMissing(
                AttendanceRuleKey.SHORT_WORK_DURATION_MINUTES,
                "Short Work Duration Minutes",
                "Minimum worked minutes required before short-duration flag",
                true,
                60,
                30,
                RiskSeverity.HIGH
        );

        createIfMissing(
                AttendanceRuleKey.TOO_MANY_CORRECTIONS_LIMIT,
                "Too Many Corrections Limit",
                "Max correction requests allowed in window",
                true,
                3,
                15,
                RiskSeverity.MEDIUM
        );

        createIfMissing(
                AttendanceRuleKey.TOO_MANY_CORRECTIONS_WINDOW_DAYS,
                "Corrections Window Days",
                "Lookback days for correction count",
                true,
                30,
                0,
                RiskSeverity.MEDIUM
        );

        createIfMissing(
                AttendanceRuleKey.WEB_ATTENDANCE_DEPENDENCY_LIMIT,
                "Web Attendance Dependency Limit",
                "Max web punches allowed in window",
                true,
                3,
                15,
                RiskSeverity.MEDIUM
        );

        createIfMissing(
                AttendanceRuleKey.WEB_ATTENDANCE_DEPENDENCY_WINDOW_DAYS,
                "Web Attendance Dependency Window",
                "Lookback days for web punches",
                true,
                14,
                0,
                RiskSeverity.MEDIUM
        );

        createIfMissing(
                AttendanceRuleKey.INVALID_OT_MINUTES_LIMIT,
                "Invalid OT Minutes Limit",
                "Max overtime minutes allowed before invalid OT flag is raised",
                true,
                180,
                50,
                RiskSeverity.CRITICAL
        );

        createIfMissing(
                AttendanceRuleKey.REVIEW_TRUST_THRESHOLD,
                "Review Trust Threshold",
                "Trust score below this requires admin review",
                true,
                60,
                0,
                RiskSeverity.MEDIUM
        );

        createIfMissing(
                AttendanceRuleKey.HIGH_RISK_TRUST_THRESHOLD,
                "High Risk Threshold",
                "Trust score below this considered high risk",
                true,
                40,
                0,
                RiskSeverity.HIGH
        );

        System.out.println("Attendance intelligence rules bootstrap completed");
    }

    private void createIfMissing(
            AttendanceRuleKey key,
            String name,
            String description,
            boolean enabled,
            Integer threshold,
            Integer score,
            RiskSeverity severity
    ) {
        if (attendanceRuleRepository.findByRuleKey(key).isPresent()) return;

        attendanceRuleRepository.save(
                AttendanceRule.builder()
                        .ruleKey(key)
                        .ruleName(name)
                        .description(description)
                        .enabled(enabled)
                        .thresholdValue(threshold)
                        .scoreImpact(score)
                        .severity(severity)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }
}