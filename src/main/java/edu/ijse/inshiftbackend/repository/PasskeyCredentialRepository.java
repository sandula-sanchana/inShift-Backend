package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.Employee;
import edu.ijse.inshiftbackend.entity.PasskeyCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, Long> {

    boolean existsByCredentialId(String credentialId);

    List<PasskeyCredential> findAllByEmployeeEmailAndActiveTrue(String email);

    Optional<PasskeyCredential> findByCredentialIdAndEmployeeEmployeeIdAndActiveTrue(
            String credentialId,
            Long employeeId
    );

    List<PasskeyCredential> findAllByCredentialIdAndActiveTrue(String credentialId);


    Optional<PasskeyCredential> findByCredentialIdAndActiveTrue(String credentialId);

    List<PasskeyCredential> findByEmployeeAndActiveTrue(Employee employee);

    Optional<PasskeyCredential> findTopByEmployeeAndActiveTrueOrderByCreatedAtDesc(Employee employee);

    long countByEmployeeAndActiveTrue(Employee employee);
}