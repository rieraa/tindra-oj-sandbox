package com.aneto.tindraojsandbox.exceptionhandle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SandBoxResponse<T> {

    private int code;
    private String message;
    private T data;

    public SandBoxResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
