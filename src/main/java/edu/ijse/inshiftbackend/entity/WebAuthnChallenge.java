package edu.ijse.inshiftbackend.entity;

import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class WebAuthnChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String challenge;

    private boolean used;

    @ManyToOne
    private Employee employee;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private WebAuthnChallengePurpose purpose;

}
