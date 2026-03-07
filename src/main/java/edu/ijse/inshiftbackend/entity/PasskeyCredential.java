package edu.ijse.inshiftbackend.entity;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, length = 500)
    private String credentialId;

    @Lob
    @Column(nullable = false)
    private String publicKey;

    private Long signCount;

    private String deviceName;

    @Column(nullable = false)
    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime lastUsedAt;
}