package com.aneto.tindraojsandbox.sandbox;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.aneto.tindraojsandbox.model.ExecuteCodeRequest;
import com.aneto.tindraojsandbox.model.ExecuteCodeResponse;
import com.aneto.tindraojsandbox.model.ExecuteOutPut;
import com.aneto.tindraojsandbox.model.JudgeInfo;
import com.aneto.tindraojsandbox.util.ProcessUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaSandBoxImpl implements SandBox {

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
        executeCodeRequest.setInput(Arrays.asList("1 2", "2 3"));
        //executeCodeRequest.setCode("public class Main { public static void main(String[] args) { System.out.println(\"Hello World\");System.out.println(\"结果 World\"); } }");
        executeCodeRequest.setCode(ResourceUtil.readStr("danger/Main.java", StandardCharsets.UTF_8));
        executeCodeRequest.setLanguage("java");
        javaSandBox.executeCode(executeCodeRequest);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInput();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        //  校验代码中是否包含黑名单中的命令
        FoundWord foundWord = WORD_TREE.matchWord(code);
        if (foundWord != null) {
            System.out.println("包含禁止词：" + foundWord.getFoundWord());
            return null;
        }

        // 1、新增所有用户的代码文件夹
        String userDir = System.getProperty("user.dir");
        String userSHome = userDir + File.separator + USER_HOME;
        if (!FileUtil.exist(userSHome)) {
            FileUtil.mkdir(userSHome);
        }

        // 2、新增单个用户的代码文件夹并写入代码文件
        String userPath = userSHome + File.separator + UUID.randomUUID();
        String filePath = userPath + File.separator + FILE_NAME;// 代码所处目录
        // 写入代码文件
        File codeFile = FileUtil.writeString(code, filePath, StandardCharsets.UTF_8);

        // 3、编译代码,生成class文件
        String compileCommand = String.format("javac -encoding utf-8 %s", codeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCommand);
            ExecuteOutPut executeOutPut = ProcessUtil.execute(compileProcess, "编译");
            System.out.println("🚀 ~ file:JavaSandBoxImpl.java method:executeCode line:57 -----executeOutPut:" + executeOutPut);
        } catch (IOException | InterruptedException e) {
            return getErrorResponse(e);
        }

        // 4、运行代码
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
                System.out.println("🚀 ~ file:JavaSandBoxImpl.java method:executeCode line:65 -----executeOutPut:" + executeOutPut);
            } catch (IOException | InterruptedException e) {
                return getErrorResponse(e);
            }
        }

        // 5、返回执行结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(1);
        List<String> outPutList = new ArrayList<>();// 每个用例对应的输出结果
        Long maxTime = 0L;
        for (ExecuteOutPut executeOutPut : executeOutPutList) {
            String error = executeOutPut.getError();
            if (error != null && !error.isEmpty()) {
                executeCodeResponse.setMessage("一个或多个用例执行失败");
                executeCodeResponse.setStatus(3);
                outPutList.add(error);// 存入当前用例的错误信息
                continue;
            }
            outPutList.add(executeOutPut.getOutPut());
            Long time = executeOutPut.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setOutput(outPutList);


        // 6、删除用户代码文件夹
        if (codeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userPath);
            System.out.println("delete " + (del ? "success" : "fail"));
        }

        return executeCodeResponse;
    }

    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(2);
        // 代码沙箱出错（可能是编译出现了错误）
        executeCodeResponse.setMessage(e.getMessage());
        return executeCodeResponse;
    }
}
