package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.PresenceCheckBiometricProof;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PresenceCheckBiometricProofRepository
        extends JpaRepository<PresenceCheckBiometricProof, Long> {

    Optional<PresenceCheckBiometricProof> findByProofTokenAndUsedFalseAndExpiresAtAfter(
            String proofToken,
            LocalDateTime now
    );
}