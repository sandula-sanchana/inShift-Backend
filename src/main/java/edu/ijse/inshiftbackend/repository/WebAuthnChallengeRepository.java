package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.WebAuthnChallenge;
import edu.ijse.inshiftbackend.entity.enums.WebAuthnChallengePurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface WebAuthnChallengeRepository extends JpaRepository<WebAuthnChallenge, Long> {

    Optional<WebAuthnChallenge> findTopByEmployeeAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            Employee employee,
            WebAuthnChallengePurpose purpose
    );
}
