package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.PresenceCheckPlanStatus;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckRiskLevel;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckSourceExpected;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckTriggerReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "presence_check_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceCheckPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "employee_id", referencedColumnName = "employeeId", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PresenceCheckTriggerReason triggerReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PresenceCheckRiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PresenceCheckSourceExpected sourceExpected;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PresenceCheckPlanStatus status;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime plannedAt;

    private LocalDateTime triggeredAt;

    @Column(nullable = false)
    private Integer dueInMinutes;

    @Column(nullable = false)
    private Integer sequenceNo;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}