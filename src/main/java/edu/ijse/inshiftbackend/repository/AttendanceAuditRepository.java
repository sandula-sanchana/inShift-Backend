package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.AttendanceAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceAuditRepository
        extends JpaRepository<AttendanceAudit, Long> {
}