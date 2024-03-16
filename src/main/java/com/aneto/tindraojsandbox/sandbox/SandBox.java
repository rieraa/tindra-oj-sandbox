package com.aneto.tindraojsandbox.sandbox;


import com.aneto.tindraojsandbox.model.ExecuteCodeRequest;
import com.aneto.tindraojsandbox.model.ExecuteCodeResponse;

public interface SandBox {

    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
