package com.aneto.tindraojsandbox.sandbox;

/**
 * 在 docker容器中运行代码
 */

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import com.aneto.tindraojsandbox.model.ExecuteCodeRequest;
import com.aneto.tindraojsandbox.model.ExecuteCodeResponse;
import com.aneto.tindraojsandbox.model.ExecuteOutPut;
import com.aneto.tindraojsandbox.model.JudgeInfo;
import com.aneto.tindraojsandbox.util.ProcessUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class JavaDockerSandBoxImpl implements SandBox {

    private static final String USER_HOME = "userHome";
    private static final String FILE_NAME = "Main.java";
    private static final Long EXCEED_LIMIT = 5000L;// 限制时间

    private static final boolean INIT = false;


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
        List<String> inputList = executeCodeRequest.getInput();
        String code = executeCodeRequest.getCode();


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

        // 4、拉取镜像
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String img = "openjdk:8-alpine";// java镜像
        if (INIT) {
            // 首次进入时创建一个新的容器
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(img);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("🚀 ~ file:JavaDockerSandBoxImpl.java method:executeCode line:99 -----镜像拉取错误:");
                throw new RuntimeException(e);
            }
        }
        System.out.println("下载完成");

        // 5、创建容器
        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(img);
        HostConfig hostConfig = new HostConfig();
        // 绑定用户代码文件夹到docker中
        hostConfig.setBinds(new Bind(userPath, new Volume("/app")));
        // 限制内存
        hostConfig.withMemory(100 * 1000 * 1000L);
        // 设置CPU
        hostConfig.withCpuCount(1L);
        // 内存交换
        hostConfig.withMemorySwap(1000L);

        CreateContainerResponse createConfigResponse = createContainerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withAttachStdin(true)
                .withTty(true)
                .exec();
        // 6、启动容器
        String containerId = createConfigResponse.getId();
        System.out.println("创建容器id：" + containerId);

        dockerClient.startContainerCmd(containerId).exec();
        // 7、执行用户代码
        //docker exec clever_mahavira java -cp /app Main 1 3
        ArrayList<ExecuteOutPut> executeOutPutList = new ArrayList<>();
        // 每个输入参数的执行输出
        ExecuteOutPut executeOutPut = new ExecuteOutPut();
        StopWatch stopWatch = new StopWatch();
        final String[] message = {null};
        final String[] error = {null};
        final Long[] memory = {0L};
        long time = 0L;
        for (String input : inputList) {
            // 拼接执行命令，切割输入参数
            String[] inputs = input.split(" ");
            String[] execCommand = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputs);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(execCommand)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .withAttachStdin(true)
                    //.withTty(true)
                    .exec();
            String execId = execCreateCmdResponse.getId();

            // 判断是否超时
            final boolean[] timeout = {true};
            // 程序执行的回调
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        error[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + error[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            // 获取占用的内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void close() throws IOException {
                }

                @Override
                public void onStart(Closeable closeable) {
                }

                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    memory[0] = Math.max(statistics.getMemoryStats().getUsage(), memory[0]);
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onComplete() {
                }
            });
            statsCmd.exec(statisticsResultCallback);


            try {
                stopWatch.start();
                dockerClient
                        .execStartCmd(execId)
                        .exec(execStartResultCallback)
                        // 这里如果设置超时时间 会导致丢失输出结果
                        .awaitCompletion();
                stopWatch.stop();
                time = stopWatch.getTotalTimeMillis();
                statsCmd.close();


            } catch (InterruptedException e) {
                System.out.println("🚀 ~ file:JavaDockerSandBoxImpl.java method:executeCode line:150 -----执行用户代码错误:");
                throw new RuntimeException(e);
            }
            System.out.println("耗时：" + time + " ms");
            executeOutPut.setOutPut(message[0]);
            executeOutPut.setError(error[0]);
            executeOutPut.setTime(time);
            executeOutPut.setMemory(memory[0]);
            System.out.println("🚀 ~ file:JavaDockerSandBoxImpl.java method:executeCode line:214 -----executeOutPut:" + executeOutPut);
            executeOutPutList.add(executeOutPut);


        }
        System.out.println("🚀 ~ file:JavaDockerSandBoxImpl.java method:executeCode line:219 -----executeOutPutList:" + executeOutPutList);


        // 8、返回执行结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(1);
        List<String> outPutList = new ArrayList<>();// 每个用例对应的输出结果
        Long maxTime = 0L;
        for (ExecuteOutPut executeOutPutItem : executeOutPutList) {
            String errorMsg = executeOutPutItem.getError();
            if (errorMsg != null && !errorMsg.isEmpty()) {
                executeCodeResponse.setMessage("一个或多个用例执行失败");
                executeCodeResponse.setStatus(3);
                outPutList.add(errorMsg);// 存入当前用例的错误信息
                continue;
            }
            outPutList.add(executeOutPutItem.getOutPut());
            Long timeConsume = executeOutPutItem.getTime();
            if (timeConsume != null) {
                maxTime = Math.max(maxTime, timeConsume);
            }
        }
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setOutput(outPutList);
        System.out.println("🚀 ~ file:JavaDockerSandBoxImpl.java method:executeCode line:187 -----executeCodeResponse:" + executeCodeResponse);
        // 9、删除容器
        // 停止容器
        dockerClient.stopContainerCmd(containerId).exec();
        RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(containerId);
        removeContainerCmd.exec();
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
