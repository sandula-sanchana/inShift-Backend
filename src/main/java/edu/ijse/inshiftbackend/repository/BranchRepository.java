package edu.ijse.inshiftbackend.repository;

import edu.ijse.inshiftbackend.dto.BranchDTO;
import edu.ijse.inshiftbackend.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch,Long> {

}
