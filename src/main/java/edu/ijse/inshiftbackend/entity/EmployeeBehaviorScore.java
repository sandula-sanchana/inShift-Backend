package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.RiskSeverity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "employee_behavior_score",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "employee_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeBehaviorScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Column(nullable = false)
    private Integer currentRiskScore;

    @Column(nullable = false)
    private Integer currentTrustScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RiskSeverity currentRiskLevel;

    @Column(nullable = false)
    private Integer totalFlagsSeen;

    @Column(nullable = false)
    private Integer totalHighRiskDays;

    private LocalDateTime lastEvaluatedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}