package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.WebAuthnChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebAuthnChallengeRepository extends JpaRepository<WebAuthnChallenge, Long> {

}
