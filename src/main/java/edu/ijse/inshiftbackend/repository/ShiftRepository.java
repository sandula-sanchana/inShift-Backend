package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
}
