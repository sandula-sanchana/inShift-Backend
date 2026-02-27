package edu.ijse.inshiftbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AuthResponseDTO {
    private String access_token;
    private String role;
}
