package edu.ijse.inshiftbackend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PubKeyCredParamDTO {
    private String type;
    private Integer alg;
}