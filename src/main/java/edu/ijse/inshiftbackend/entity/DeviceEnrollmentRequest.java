package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.DeviceEnrollmentRequestStatus;
import edu.ijse.inshiftbackend.entity.enums.DeviceEnrollmentRequestType;
import edu.ijse.inshiftbackend.entity.enums.DeviceTrustType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "device_enrollment_request")
public class DeviceEnrollmentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_device_id")
    private EmployeeDevice employeeDevice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DeviceEnrollmentRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceEnrollmentRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DeviceTrustType requestedTrustType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DeviceTrustType approvedTrustType;

    @Column(length = 150)
    private String requestedDeviceName;

    @Column(length = 500)
    private String requestedUserAgent;

    @Column(length = 100)
    private String requestedPlatform;

    @Column(length = 100)
    private String requestedBrowser;

    @Column(length = 100)
    private String requestedIpAddress;

    @Column(length = 255)
    private String requestReason;

    @Column(nullable = false)
    private Integer riskScoreImpact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "existing_credential_id")
    private PasskeyCredential existingCredentialToReplace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Employee approvedBy;

    private LocalDateTime approvedAt;

    @Column(length = 255)
    private String adminComment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime completedAt;
}