package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.BranchDTO;
import edu.ijse.inshiftbackend.service.BranchService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/branches")
@CrossOrigin
public class AdminBranchController {

    private final BranchService branchService;

    @PostMapping
    public ResponseEntity<APIResponse<String>> createBranch(@RequestBody @Valid BranchDTO branchDTO) {
        branchService.createBranch(branchDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new APIResponse<>(
                        201,
                        "Branch saved successfully",
                        null
                )
        );
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<List<BranchDTO>> getAllBranches() {
        return new APIResponse<>(
                200,
                "OK",
                branchService.getAllBranch()
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<BranchDTO> getBranchById(@PathVariable Long id) {
        return new APIResponse<>(
                200,
                "Branch fetched successfully",
                branchService.getBranchById(id)
        );
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<String> updateBranch(
            @PathVariable Long id,
            @RequestBody @Valid BranchDTO branchDTO
    ) {
        branchService.updateBranch(id, branchDTO);

        return new APIResponse<>(
                200,
                "Branch updated successfully",
                null
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public APIResponse<String> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);

        return new APIResponse<>(
                200,
                "Branch deleted successfully",
                null
        );
    }
}