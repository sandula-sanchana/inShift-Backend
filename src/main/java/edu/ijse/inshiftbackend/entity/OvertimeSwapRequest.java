package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.OvertimeSwapStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "overtime_swap_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeSwapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "overtime_assignment_id", nullable = false)
    private OvertimeAssignment overtimeAssignment;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "from_employee_id", nullable = false)
    private Employee fromEmployee;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "to_employee_id", nullable = false)
    private Employee toEmployee;

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OvertimeSwapStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;

    @PrePersist
    public void prePersist() {
        if (status == null) status = OvertimeSwapStatus.PENDING;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}