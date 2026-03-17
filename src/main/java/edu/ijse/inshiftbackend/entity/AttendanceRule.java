package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.AttendanceRuleKey;
import edu.ijse.inshiftbackend.entity.enums.RiskSeverity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "attendance_rule")
public class AttendanceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 100)
    private AttendanceRuleKey ruleKey;

    @Column(nullable = false, length = 150)
    private String ruleName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean enabled;

    private Integer thresholdValue;

    private Integer scoreImpact;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RiskSeverity severity;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}