package edu.ijse.inshiftbackend.exception;

import edu.ijse.inshiftbackend.exception.custom.BadRequestException;
import edu.ijse.inshiftbackend.exception.custom.ResourceNotFoundException;
import edu.ijse.inshiftbackend.util.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<String>>  generalExceptionHandler(Exception exception) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new APIResponse<>(
                        500,
                        "Internal Server error",
                        null
                )
        );

    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<APIResponse<String>> badRequestExceptionHandler(BadRequestException e) {
        return new ResponseEntity<>(new APIResponse<>(400, e.getMessage(), null), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<APIResponse<String>> resourceNotFoundExceptionHandler(ResourceNotFoundException e) {
        return new ResponseEntity<>(new APIResponse<>(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
    }

}
