package com.aneto.tindraojsandbox.sandbox;

/**
 * 在 docker容器中运行代码
 */

import cn.hutool.core.io.resource.ResourceUtil;
import com.aneto.tindraojsandbox.model.ExecuteCodeRequest;
import com.aneto.tindraojsandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class JavaDockerSandBoxImpl extends JavaCodeSandboxTemplate {


    public static void main(String[] args) {
        JavaDockerSandBoxImpl javaSandBox = new JavaDockerSandBoxImpl();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInput(Arrays.asList("1 2", "2 3"));
        //executeCodeRequest.setCode("public class Main { public static void main(String[] args) { System.out.println(\"Hello World\");System.out.println(\"结果 World\"); } }");
        executeCodeRequest.setCode(ResourceUtil.readStr("code/Main.java", StandardCharsets.UTF_8));
        executeCodeRequest.setLanguage("java");
        javaSandBox.executeCode(executeCodeRequest);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
