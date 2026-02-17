package edu.ijse.inshiftbackend.exception.custom;

public class BadRequestException extends  RuntimeException{

    public BadRequestException(String msg){
        super(msg);
    }
}
