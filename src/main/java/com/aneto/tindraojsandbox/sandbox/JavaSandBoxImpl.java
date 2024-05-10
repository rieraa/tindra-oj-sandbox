package com.aneto.tindraojsandbox.sandbox;


import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.aneto.tindraojsandbox.exceptionhandle.SandBoxException;
import com.aneto.tindraojsandbox.model.ExecuteCodeRequest;
import com.aneto.tindraojsandbox.model.ExecuteCodeResponse;
import com.aneto.tindraojsandbox.model.ExecuteOutPut;
import com.aneto.tindraojsandbox.util.ProcessUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class JavaSandBoxImpl extends JavaCodeSandboxTemplate {

    private static final String USER_HOME = "userHome";
    private static final String FILE_NAME = "Main.java";

    // 限制时间
    private static final Long EXCEED_LIMIT = 5000L;

    //代码中禁止出现的关键字
    private static final List<String> banList = Arrays.asList("Files", "exec");

    private static final WordTree WORD_TREE;

    static {
        // 初始化字典树
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(banList);
    }

    public static void main(String[] args) {
        JavaSandBoxImpl javaSandBox = new JavaSandBoxImpl();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInput(Arrays.asList("1 2"));
        //executeCodeRequest.setCode("public class Main { public static void main(String[] args) { System.out.println(\"Hello World\");System.out.println(\"结果 World\"); } }");
        executeCodeRequest.setCode(ResourceUtil.readStr("danger/Main.java", StandardCharsets.UTF_8));
        executeCodeRequest.setLanguage("java");
        javaSandBox.executeCode(executeCodeRequest);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        //  校验代码中是否包含黑名单中的命令
        FoundWord foundWord = WORD_TREE.matchWord(code);

        if (foundWord != null) {
            log.error("包含禁止词：" + foundWord.getFoundWord());
            return null;
        }


        return super.executeCode(executeCodeRequest);
    }


    @Override
    public List<ExecuteOutPut> runCode(File codeFile, List<String> inputList) {
        String userPath = codeFile.getParentFile().getAbsolutePath();
        List<ExecuteOutPut> executeOutPutList = new ArrayList<>();
        for (String input : inputList) {
            String runCommand = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userPath, input);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCommand);
                // 限制时间 新增线程执行5s 5秒后杀死代码执行进程
                new Thread(() -> {
                    try {
                        Thread.sleep(EXCEED_LIMIT);
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                ExecuteOutPut executeOutPut = ProcessUtil.execute(runProcess, "运行");
                executeOutPutList.add(executeOutPut);
            } catch (IOException | InterruptedException e) {
                throw new SandBoxException(40004, "RUNTIME_ERROR");

            }
        }
        return executeOutPutList;
    }
}
