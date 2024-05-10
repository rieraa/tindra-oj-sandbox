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
import javax.servlet.http.HttpServletRequest;

@RestController
public class codeController {

    @Resource
    private JavaSandBoxImpl javaSandBoxImpl;

    @Resource
    private JavaDockerSandBoxImpl javaDockerSandBoxImpl;


    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "secret";



    @PostMapping("/execute/native")
    public SandBoxResponse<ExecuteCodeResponse> executeCodeNative(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            return new SandBoxResponse<>(401, "Unauthorized");
        }
        ExecuteCodeResponse executeCodeResponse = javaSandBoxImpl.executeCode(executeCodeRequest);
        return new SandBoxResponse<>(200, "success", executeCodeResponse);
    }

    // todo 并发
    @PostMapping("/execute/docker")
    public SandBoxResponse<ExecuteCodeResponse> executeCodeDocker(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            return new SandBoxResponse<>(401, "Unauthorized");
        }
        ExecuteCodeResponse executeCodeResponse = javaDockerSandBoxImpl.executeCode(executeCodeRequest);
        return new SandBoxResponse<>(200, "success", executeCodeResponse);
    }
}
