package edu.ijse.inshiftbackend.controllers;

import edu.ijse.inshiftbackend.dto.BranchDTO;
import edu.ijse.inshiftbackend.service.BranchService;
import edu.ijse.inshiftbackend.util.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("api/v1/branch")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;



     @PostMapping
     public  ResponseEntity<APIResponse<String>> createBranch(@RequestBody @Valid BranchDTO branchDTO){

         return ResponseEntity.status(HttpStatus.CREATED).body(
                 new APIResponse<>(201,
                         "Branch Saved Successfully",
                         null)
         );

     }

}
