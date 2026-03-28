package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.PresenceCheck;
import edu.ijse.inshiftbackend.entity.enums.PresenceCheckStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PresenceCheckRepository extends JpaRepository<PresenceCheck, Long> {

    Optional<PresenceCheck> findFirstByEmployeeEmployeeIdAndStatusOrderByCreatedAtDesc(
            Long employeeId,
            PresenceCheckStatus status
    );

    Optional<PresenceCheck> findFirstByEmployeeEmployeeIdAndStatusInOrderByCreatedAtDesc(
            Long employeeId,
            Collection<PresenceCheckStatus> statuses
    );

    Optional<PresenceCheck> findFirstByEmployeeEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    boolean existsByEmployeeEmployeeIdAndStatusIn(
            Long employeeId,
            Collection<PresenceCheckStatus> statuses
    );

    long countByEmployeeEmployeeIdAndStatusIn(
            Long employeeId,
            Collection<PresenceCheckStatus> statuses
    );

    List<PresenceCheck> findByStatusOrderByCreatedAtDesc(PresenceCheckStatus status);

    List<PresenceCheck> findByStatusInOrderByCreatedAtDesc(Collection<PresenceCheckStatus> statuses);

    List<PresenceCheck> findAllByOrderByCreatedAtDesc();

    List<PresenceCheck> findByEmployeeEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<PresenceCheck> findByStatusAndDueAtBefore(PresenceCheckStatus status, LocalDateTime now);

    List<PresenceCheck> findByStatus(PresenceCheckStatus status);

    List<PresenceCheck> findByStatusAndDueAtIsNotNullAndDueAtBefore(
            PresenceCheckStatus status,
            LocalDateTime now
    );
}