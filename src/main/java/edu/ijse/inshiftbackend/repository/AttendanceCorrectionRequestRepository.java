package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.AttendanceCorrectionRequest;
import edu.ijse.inshiftbackend.entity.enums.CorrectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceCorrectionRequestRepository
        extends JpaRepository<AttendanceCorrectionRequest, Long> {

    List<AttendanceCorrectionRequest> findAllByEmployeeEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<AttendanceCorrectionRequest> findAllByStatusOrderByCreatedAtDesc(CorrectionStatus status);
}