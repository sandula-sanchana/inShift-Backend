package edu.ijse.inshiftbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PasskeyCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Employee employee;

    private String credentialId;

    private String publicKey;

    private Long signCount;

    private String deviceName;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime lastUsedAt;

}
