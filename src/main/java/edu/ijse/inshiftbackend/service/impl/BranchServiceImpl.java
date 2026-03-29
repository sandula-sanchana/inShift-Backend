package edu.ijse.inshiftbackend.service.impl;

import edu.ijse.inshiftbackend.dto.BranchDTO;
import edu.ijse.inshiftbackend.entity.Branch;
import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.repository.BranchRepository;
import edu.ijse.inshiftbackend.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final ModelMapper modelMapper;

    @Override
    public void createBranch(BranchDTO branchDTO) {
        if (branchDTO == null) {
            throw new BadRequestException("branchDTO is null");
        }

        Branch branch = modelMapper.map(branchDTO, Branch.class);
        branch.setBranchId(null);

        branchRepository.save(branch);
    }

    @Override
    public List<BranchDTO> getAllBranch() {
        return branchRepository.findAll()
                .stream()
                .map(branch -> modelMapper.map(branch, BranchDTO.class))
                .toList();
    }

    @Override
    public BranchDTO getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        return modelMapper.map(branch, BranchDTO.class);
    }

    @Override
    public void updateBranch(Long id, BranchDTO branchDTO) {
        if (branchDTO == null) {
            throw new BadRequestException("branchDTO is null");
        }

        Branch existing = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        existing.setBranchCode(branchDTO.getBranchCode());
        existing.setBranchName(branchDTO.getBranchName());
        existing.setAddressLine1(branchDTO.getAddressLine1());
        existing.setAddressLine2(branchDTO.getAddressLine2());
        existing.setCity(branchDTO.getCity());
        existing.setDistrict(branchDTO.getDistrict());
        existing.setProvince(branchDTO.getProvince());
        existing.setLatitude(branchDTO.getLatitude());
        existing.setLongitude(branchDTO.getLongitude());
        existing.setRadiusMeters(branchDTO.getRadiusMeters());
        existing.setContactNumber(branchDTO.getContactNumber());
        existing.setEmail(branchDTO.getEmail());
        existing.setActive(branchDTO.getActive());

        branchRepository.save(existing);
    }

    @Override
    public void deleteBranch(Long id) {
        if (!branchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Branch not found");
        }

        branchRepository.deleteById(id);
    }
}