package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.DeviceApprovalStatus;
import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_device",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "deviceFingerprint")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", referencedColumnName = "employeeId", nullable = false)
    private Employee employee;

    @Column(nullable = false, unique = true, length = 255)
    private String deviceFingerprint;

    @Column(length = 150)
    private String deviceName;

    @Column(length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceTrustType requestedTrustType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DeviceTrustType approvedTrustType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceApprovalStatus approvalStatus;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}