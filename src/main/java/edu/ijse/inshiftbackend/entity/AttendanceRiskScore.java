package edu.ijse.inshiftbackend.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_risk_score",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "attendanceDate"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Column(nullable = false)
    private Integer riskScore;

    @Column(nullable = false)
    private Integer trustScore;

    @Column(nullable = false)
    private Integer totalFlags;

    @Column(nullable = false)
    private Boolean requiresReview;

    @Column(nullable = false)
    private Boolean highRisk;

    private LocalDateTime calculatedAt;

    @PrePersist
    @PreUpdate
    public void stamp() {
        calculatedAt = LocalDateTime.now();
    }
}