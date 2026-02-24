package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.BranchDTO;
import edu.ijse.inshiftbackend.service.BranchService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/branch")
@CrossOrigin(origins = "http://localhost:5173")
public class BranchController {

    private final BranchService branchService;



     @PostMapping
     public  ResponseEntity<APIResponse<String>> createBranch(@RequestBody @Valid BranchDTO branchDTO){
         branchService.createBranch(branchDTO);
         return ResponseEntity.status(HttpStatus.CREATED).body(
                 new APIResponse<>(201,
                         "Branch Saved Successfully",
                         null)
         );

     }
     @GetMapping
     @ResponseStatus(HttpStatus.CREATED)
     public APIResponse<List<BranchDTO>> updateBranch(@RequestBody @Valid BranchDTO branchDTO){
         List<BranchDTO> branches=branchService.getAllBranch();
         return new APIResponse<>(
                 200,
                 "Ok",
                 branches
         );
     }

}
