package com.lhs.common.exception;

public class InterfaceException extends RuntimeException{

    private String message;
    private Integer code;

    public InterfaceException(String message,Integer code) {
        this(message);
        this.message = message;
        this.code = code;
    }



    private InterfaceException(String message) {
        super(message);
    }


}
