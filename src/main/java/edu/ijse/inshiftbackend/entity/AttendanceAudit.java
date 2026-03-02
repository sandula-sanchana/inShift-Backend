package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_audit",
        indexes = {
                @Index(name = "idx_audit_att", columnList = "attendance_id"),
                @Index(name = "idx_audit_time", columnList = "action_time")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_id")
    private AttendanceRecord attendance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuditAction action; // CREATE / APPROVE / REJECT / UPDATE

    @Column(nullable = false, length = 20)
    private String doneByRole; // EMPLOYEE / ADMIN / SYSTEM

    @Column(nullable = false)
    private Long doneByUserId;

    @Column(name = "action_time", nullable = false)
    private LocalDateTime actionTime;

    @Column(length = 500)
    private String note;

    @PrePersist
    public void prePersist() {
        if (actionTime == null) {
            actionTime = LocalDateTime.now();
        }
    }
}