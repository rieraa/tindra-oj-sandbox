package com.aneto.tindraojsandbox.exceptionhandle;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SandBoxException.class)
    @ResponseBody
    public SandBoxResponse handleCustomException(SandBoxException ex) {
        return new SandBoxResponse(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public SandBoxResponse handleException(Exception ex) {
        return new SandBoxResponse(500, "Server Error");
    }
}
