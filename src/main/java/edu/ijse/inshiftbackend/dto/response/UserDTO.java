package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private String id;          // Base64URL encoded user handle
    private String name;        // usually email or username
    private String displayName; // full name
}