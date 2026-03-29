package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeLookupDTO {
    private Long employeeId;
    private String empCode;
    private String fullName;
    private String email;
}