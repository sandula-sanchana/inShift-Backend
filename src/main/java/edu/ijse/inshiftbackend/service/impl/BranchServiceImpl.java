package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.BranchDTO;
import edu.ijse.inshiftbackend.entity.Branch;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.repository.BranchRepository;
import edu.ijse.inshiftbackend.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final ModelMapper modelMapper;

    @Override
    public void   createBranch(BranchDTO branchDTO) {

        if (branchDTO==null){
            throw  new BadRequestException("branchDTO is null");
        }
         branchRepository.save(modelMapper.map(branchDTO, Branch.class));
    }
}
