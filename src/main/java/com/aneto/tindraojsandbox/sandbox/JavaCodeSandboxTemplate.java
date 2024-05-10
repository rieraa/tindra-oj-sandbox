package com.aneto.tindraojsandbox.sandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import com.aneto.tindraojsandbox.exceptionhandle.SandBoxException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public abstract class JavaCodeSandboxTemplate implements SandBox {

    // 在本地存储用户代码的文件夹
    private static final String USER_HOME = "userHome";
    // 用户代码文件名
    private static final String FILE_NAME = "Main.java";
    // 代码超时时间
    private static final Long EXCEED_LIMIT = 10000L;// 限制时间
    // 是否首次进入
    private static final boolean INIT = false;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        // 获取用户输入用例
        List<String> inputList = executeCodeRequest.getInput();
        // 获取用户代码
        String code = executeCodeRequest.getCode();

        // 1、保存用户代码
        File codeFile = saveUserCode(code);
        // 2、编译代码
        ExecuteOutPut compileOutPut = compileCode(codeFile);
        log.info("🚀 ~ file:JavaCodeSandboxTemplate.java method:executeCode line:94 -----compileOutPut:" + compileOutPut);
        // 3、运行代码
        List<ExecuteOutPut> executeOutPutList = runCode(codeFile, inputList);
        // 4、整理输出结果
        ExecuteCodeResponse organizeOutputResults = organizeOutputResults(executeOutPutList);
        log.info("🚀 ~ file:JavaCodeSandboxTemplate.java method:executeCode line:55 -----organizeOutputResults:" + organizeOutputResults);
        // 5、删除文件
        boolean deleted = deleteCodeFile(codeFile);
        if (!deleted) {
            log.error("deleteFile error, userCodeFilePath = " + codeFile.getAbsolutePath());
        }


        return organizeOutputResults;


    }


    /**
     * 1、保存用户代码
     *
     * @param code 用户代码
     * @return
     */
    public File saveUserCode(String code) {
        // 所有用户的代码文件夹
        String userDir = System.getProperty("user.dir");
        String userSHome = userDir + File.separator + USER_HOME;
        // 若不存在则新增文件
        if (!FileUtil.exist(userSHome)) {
            FileUtil.mkdir(userSHome);
        }

        // 单个用户的代码文件夹
        String userPath = userSHome + File.separator + UUID.randomUUID();
        // 代码所处目录
        String filePath = userPath + File.separator + FILE_NAME;
        // 写入代码文件
        return FileUtil.writeString(code, filePath, StandardCharsets.UTF_8);
    }

    /**
     * 2、编译代码
     *
     * @param codeFile 代码文件
     */
    public ExecuteOutPut compileCode(File codeFile) {
        // 编译代码,生成class文件
        String compileCommand = String.format("javac -encoding utf-8 %s", codeFile.getAbsolutePath());
        try {
            // 返回一个Process 对象，表示正在执行的进程。
            Process compileProcess = Runtime.getRuntime().exec(compileCommand);
            // 调用工具类，传入正在执行的进程，返回执行结果
            ExecuteOutPut executeOutPut = ProcessUtil.execute(compileProcess, "编译");
            if (executeOutPut.getExitCode() != 0) {
                throw new SandBoxException(40002, "COMPILE_ERROR");
            }
            return executeOutPut;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 3、运行代码
     */
    public List<ExecuteOutPut> runCode(File codeFile, List<String> inputList) {
        // 代码所处目录
        String userPath = codeFile.getParentFile().getAbsolutePath();


        // 拉取镜像
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String img = "openjdk:8-alpine";// java镜像
        if (!INIT) {

            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(img);

            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    log.info("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                log.error("🚀 ~ file:JavaDockerSandBoxImpl.java method:executeCode line:99 -----镜像拉取错误:");
                throw new SandBoxException(40000, "镜像拉取错误");
            }
        }
        log.info("镜像" + img + "下载完成");

        // 创建容器
        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(img);
        // 容器创建配置
        HostConfig hostConfig = new HostConfig();
        // 映射用户代码文件夹到docker中
        hostConfig.setBinds(new Bind(userPath, new Volume("/app")));
        // 限制内存
        hostConfig.withMemory(100 * 1000 * 1000L);
        // 设置CPU
        hostConfig.withCpuCount(1L);
        // 内存交换
        hostConfig.withMemorySwap(1000L);
        //创建容器
        CreateContainerResponse createConfigResponse = createContainerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withAttachStdin(true)
                .withTty(true)
                .exec();
        String containerId = createConfigResponse.getId();
        log.info("创建容器id：" + containerId);
        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();
        // 执行用户代码
        // 所有用例的输出结果
        ArrayList<ExecuteOutPut> executeOutPutList = new ArrayList<>();
        StopWatch stopWatch = new StopWatch();
        for (String input : inputList) {
            // 拼接执行命令，切割输入参数
            //docker exec clever_mahavira java -cp /app Main 1 3
            String[] inputs = input.split(" ");
            String[] execCommand = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputs);
            // 每个输入参数的执行输出
            ExecuteOutPut executeOutPut = new ExecuteOutPut();
            final String[] message = {null};
            final String[] error = {null};
            final Long[] memory = {0L};
            long time = 0L;
            // 执行命令
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
                        log.error("输出错误结果：" + error[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        log.info("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            // 获取占用的内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            // 内存输出控制
            final boolean[] stop = {false};
            // 获取内存占用回调
            ResultCallback<Statistics> statisticsResultCallback = new ResultCallback<Statistics>() {

                @Override
                public void close() throws IOException {
                    stop[0] = false;
                }

                @Override
                public void onStart(Closeable closeable) {
                    stop[0] = true;
                }

                @Override
                public void onNext(Statistics statistics) {
                    if (stop[0]) {
                        log.info("内存占用：" + statistics.getMemoryStats().getUsage());
                        memory[0] = Math.max(statistics.getMemoryStats().getUsage(), memory[0]);
                    }

                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onComplete() {
                    stop[0] = false;
                }
            };
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
                log.error("🚀 ~ file:JavaDockerSandBoxImpl.java method:executeCode line:150 -----执行用户代码错误:");
                throw new SandBoxException(40001, "执行用户代码错误");
            }
            log.info("耗时：" + time + " ms");
            executeOutPut.setOutPut(message[0]);
            executeOutPut.setError(error[0]);
            executeOutPut.setTime(time);
            executeOutPut.setMemory(memory[0]);
            log.info("🚀 ~ file:JavaDockerSandBoxImpl.java method:executeCode line:214 -----executeOutPut:" + executeOutPut);
            executeOutPutList.add(executeOutPut);

        }
        log.info("🚀 ~ file:JavaDockerSandBoxImpl.java method:executeCode line:219 -----executeOutPutList:" + executeOutPutList);


        // 停止容器
        dockerClient.stopContainerCmd(containerId).exec();
        // 删除容器
        RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(containerId);
        removeContainerCmd.exec();

        return executeOutPutList;

    }


    /**
     * 4、整理输出结果
     *
     * @param executeOutPutList
     * @return
     */
    public ExecuteCodeResponse organizeOutputResults(List<ExecuteOutPut> executeOutPutList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(1);
        List<String> outPutList = new ArrayList<>();// 每个用例对应的输出结果
        long maxTime = 0L;
        long maxMemory = 0L;
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
            Long memory = executeOutPut.getMemory();
            if (memory != null) {
                maxMemory = Math.max(maxMemory, memory);
            }

        }
        System.out.println("🚀 ~ file:JavaCodeSandboxTemplate.java method:organizeOutputResults line:333 -----outPutList:" + outPutList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        double kilobytes = (double) maxMemory / 1024;
        judgeInfo.setMemory((long) kilobytes);
        executeCodeResponse.setOutput(outPutList);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }


    /**
     * 5、删除文件
     *
     * @param codeFile
     * @return
     */
    public boolean deleteCodeFile(File codeFile) {
        if (codeFile.getParentFile() != null) {
            boolean del = FileUtil.del(codeFile.getParentFile());
            log.info("delete " + (del ? "success" : "fail"));
            return del;
        }
        return true;
    }


    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setStatus(2);
        // 代码沙箱出错（可能是编译出现了错误）
        executeCodeResponse.setMessage(e.getMessage());
        return executeCodeResponse;
    }


}
