package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "webauthn_assertion_request")
public class WebAuthnAssertionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebAuthnChallengePurpose purpose;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String requestJson;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}