package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.WebAuthnAssertionRequest;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WebAuthnAssertionRequestRepository extends JpaRepository<WebAuthnAssertionRequest, Long> {

    Optional<WebAuthnAssertionRequest> findTopByEmployeeAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            Employee employee,
            WebAuthnChallengePurpose purpose,
            LocalDateTime now
    );
}