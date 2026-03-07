package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.PasskeyCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, Long> {

    boolean existsByCredentialId(String credentialId);

    Optional<PasskeyCredential> findByCredentialIdAndActiveTrue(String credentialId);

    List<PasskeyCredential> findAllByEmployeeEmployeeIdAndActiveTrue(Long employeeId);
}