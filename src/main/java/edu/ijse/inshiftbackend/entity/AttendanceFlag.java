package edu.ijse.inshiftbackend.entity;
import edu.ijse.inshiftbackend.entity.enums.AttendanceFlagType;
import edu.ijse.inshiftbackend.entity.enums.RiskSeverity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_flag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private LocalDate attendanceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id")
    private AttendanceRecord attendance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AttendanceFlagType flagType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskSeverity severity;

    @Column(nullable = false)
    private Integer scoreImpact;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private Boolean resolved;

    private LocalDateTime detectedAt;

    private LocalDateTime resolvedAt;

    private String resolvedNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private Employee resolvedBy;

    @PrePersist
    public void prePersist() {
        if (detectedAt == null) detectedAt = LocalDateTime.now();
        if (resolved == null) resolved = false;
    }
}