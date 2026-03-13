package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
}
