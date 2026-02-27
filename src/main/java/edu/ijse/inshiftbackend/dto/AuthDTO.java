package edu.ijse.inshiftbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class AuthDTO {
    private String email;
    private String password;
}
