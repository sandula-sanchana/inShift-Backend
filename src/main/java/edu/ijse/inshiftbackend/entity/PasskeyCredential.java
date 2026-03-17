package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.PasskeyCredentialStatus;
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
        name = "passkey_credential",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "credentialId")
        }
)
public class PasskeyCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, unique = true, length = 512)
    private String credentialId;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String publicKey;

    @Column(nullable = false)
    private Long signCount;

    @Column(length = 150)
    private String deviceName;

    @Column(length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PasskeyCredentialStatus status;

    @Column(nullable = false)
    private boolean active;

    private LocalDateTime lastUsedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime revokedAt;

    @Column(length = 250)
    private String revokedReason;
}