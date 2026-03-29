package edu.ijse.inshiftbackend.service;

import edu.ijse.inshiftbackend.dto.BranchDTO;

import java.util.List;

public interface BranchService {

    void createBranch(BranchDTO branchDTO);

    List<BranchDTO> getAllBranch();

    BranchDTO getBranchById(Long id);

    void updateBranch(Long id, BranchDTO branchDTO);

    void deleteBranch(Long id);
}