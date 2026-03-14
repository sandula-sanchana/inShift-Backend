package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.AttendanceDayStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "attendance_daily_summary",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"employee_id", "summary_date"})
        },
        indexes = {
                @Index(name = "idx_ads_date", columnList = "summary_date"),
                @Index(name = "idx_ads_emp_date", columnList = "employee_id,summary_date"),
                @Index(name = "idx_ads_branch_date", columnList = "branch_id,summary_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceDailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    private LocalDateTime firstInTime;
    private LocalDateTime lastOutTime;

    @Column(nullable = false)
    private Boolean present;

    @Column(nullable = false)
    private Boolean completed;

    @Column(nullable = false)
    private Integer lateMinutes;

    @Column(nullable = false)
    private Integer earlyLeaveMinutes;

    @Column(nullable = false)
    private Integer overtimeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttendanceDayStatus dayStatus;

    @PrePersist
    public void prePersist() {
        if (present == null) present = false;
        if (completed == null) completed = false;
        if (lateMinutes == null) lateMinutes = 0;
        if (earlyLeaveMinutes == null) earlyLeaveMinutes = 0;
        if (overtimeMinutes == null) overtimeMinutes = 0;
        if (dayStatus == null) dayStatus = AttendanceDayStatus.ABSENT;
    }
}