package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    Optional<Shift> findByIsDefaultTrueAndActiveTrue();
}
