package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PresenceCheckRepository extends JpaRepository<PresenceCheck, Long> {

    Optional<PresenceCheck> findFirstByEmployeeEmployeeIdAndStatusOrderByCreatedAtDesc(
            Long employeeId,
            PresenceCheckStatus status
    );

    List<PresenceCheck> findByStatusOrderByCreatedAtDesc(PresenceCheckStatus status);

    List<PresenceCheck> findAllByOrderByCreatedAtDesc();

    List<PresenceCheck> findByEmployeeEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<PresenceCheck> findByStatusAndDueAtBefore(PresenceCheckStatus status, LocalDateTime now);
}