package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.OvertimeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "overtime_assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate otDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer breakMinutes;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OvertimeStatus status;

    @Column(length = 1000)
    private String employeeResponseNote;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private Employee assignedBy;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (status == null) status = OvertimeStatus.ASSIGNED;
        if (assignedAt == null) assignedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}