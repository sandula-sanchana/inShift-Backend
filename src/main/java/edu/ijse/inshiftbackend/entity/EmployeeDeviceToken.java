package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.DeviceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "employee_device_token",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "fcmToken")
        }
)
public class EmployeeDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, unique = true, length = 1000)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceType deviceType;

    @Column(length = 150)
    private String deviceName;

    @Column(length = 500)
    private String userAgent;

    @Column(nullable = false)
    private boolean active;

    private LocalDateTime lastUsedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}