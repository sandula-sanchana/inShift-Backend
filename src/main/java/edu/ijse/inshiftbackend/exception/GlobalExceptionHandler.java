package edu.ijse.inshiftbackend.exception;

import edu.ijse.inshiftbackend.util.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public ResponseEntity<APIResponse<String>>  generalExceptionHandler(Exception exception) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new APIResponse<>(
                        500,
                        "Internal Server error",
                        null
                )
        );

    }

}
