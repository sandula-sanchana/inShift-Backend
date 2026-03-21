package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.PresenceCheckResponseSource;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckRiskLevel;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckSourceExpected;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckStatus;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckTriggerReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "presence_check")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "employee_id", referencedColumnName = "employeeId", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PresenceCheckTriggerReason triggerReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PresenceCheckRiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PresenceCheckStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PresenceCheckSourceExpected sourceExpected;

    @Column(length = 500)
    private String triggerDescription;

    @Column(length = 500)
    private String adminNote;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime dueAt;

    private LocalDateTime notifiedAt;

    private LocalDateTime respondedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PresenceCheckResponseSource responseSource;

    private Double responseLatitude;

    private Double responseLongitude;

    private Double responseAccuracyMeters;

    @Column(length = 255)
    private String responseLocationText;

    @Column(length = 1000)
    private String responseNote;

    private Integer responseDelaySeconds;

    @Column(nullable = false)
    private Boolean lateResponse;

    @Column(nullable = false)
    private Boolean missedResponse;

    @Column(nullable = false)
    private Boolean escalated;

    private LocalDateTime escalatedAt;

    @Column(nullable = false)
    private Integer escalationLevel;
}