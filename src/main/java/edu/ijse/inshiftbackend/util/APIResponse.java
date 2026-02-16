package edu.ijse.inshiftbackend.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class APIResponse<T> {

    private String status;
    private String message;
    private T data;

}
