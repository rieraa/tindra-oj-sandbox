package com.aneto.tindraojsandbox.controller;

import com.aneto.tindraojsandbox.exceptionhandle.SandBoxResponse;
import com.aneto.tindraojsandbox.model.ExecuteCodeRequest;
import com.aneto.tindraojsandbox.model.ExecuteCodeResponse;
import com.aneto.tindraojsandbox.sandbox.JavaDockerSandBoxImpl;
import com.aneto.tindraojsandbox.sandbox.JavaSandBoxImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class codeController {

    @Resource
    private JavaSandBoxImpl javaSandBoxImpl;

    @Resource
    private JavaDockerSandBoxImpl javaDockerSandBoxImpl;

    @PostMapping("/execute/native")
    public SandBoxResponse<ExecuteCodeResponse> executeCodeNative(@RequestBody ExecuteCodeRequest executeCodeRequest) {
        ExecuteCodeResponse executeCodeResponse = javaSandBoxImpl.executeCode(executeCodeRequest);
        return new SandBoxResponse<>(200, "success", executeCodeResponse);
    }

    @PostMapping("/execute/docker")
    public SandBoxResponse<ExecuteCodeResponse> executeCodeDocker(@RequestBody ExecuteCodeRequest executeCodeRequest) {
        ExecuteCodeResponse executeCodeResponse = javaDockerSandBoxImpl.executeCode(executeCodeRequest);
        return new SandBoxResponse<>(200, "success", executeCodeResponse);
    }
}
