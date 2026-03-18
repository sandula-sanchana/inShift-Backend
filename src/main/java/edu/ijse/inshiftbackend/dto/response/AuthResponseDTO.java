package edu.ijse.inshiftbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class AuthResponseDTO {
    private String access_token;
    private String role;
    private boolean passwordMustChange;
}
