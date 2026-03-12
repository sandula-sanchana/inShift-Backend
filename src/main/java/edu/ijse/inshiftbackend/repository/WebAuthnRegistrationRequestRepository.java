package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.WebAuthnRegistrationRequest;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebAuthnRegistrationRequestRepository
        extends JpaRepository<WebAuthnRegistrationRequest, Long> {

    Optional<WebAuthnRegistrationRequest>
    findTopByEmployeeAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            Employee employee,
            WebAuthnChallengePurpose purpose,
            java.time.LocalDateTime now
    );
}