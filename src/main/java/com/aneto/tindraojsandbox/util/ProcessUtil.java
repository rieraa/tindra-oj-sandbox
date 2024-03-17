package com.aneto.tindraojsandbox.util;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.text.StrBuilder;
import com.aneto.tindraojsandbox.model.ExecuteOutPut;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
@Slf4j
public class ProcessUtil {

    public static ExecuteOutPut execute(Process process, String operation) throws InterruptedException, IOException {
        // 创建返回对象
        ExecuteOutPut executeOutPut = createExecuteOutPut();
        // 启动计时器
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 等待编译完成
        int exitValue = process.waitFor();
        executeOutPut.setExitCode(exitValue);
        if (exitValue != 0) {
            log.error("🚀 ~ file:ProcessUtil.java method:execute line:25 -----operation:" + operation+ "失败");
            // 获取进程输出和错误输出
            executeOutPut.setError(getErrorOutput(process));
        } else {
            log.info("🚀 ~ file:ProcessUtil.java method:execute line:29 -----operation:" + operation+"成功");
            // 获取进程输出
            executeOutPut.setOutPut(getOutput(process));
        }
        stopWatch.stop();
        executeOutPut.setTime(stopWatch.getTotalTimeMillis());
        return executeOutPut;
    }

    /**
     * 创建执行输出对象
     *
     * @return executeOutPut 执行输出对象
     */
    private static ExecuteOutPut createExecuteOutPut() {
        ExecuteOutPut executeOutPut = new ExecuteOutPut();
        executeOutPut.setOutPut("");
        executeOutPut.setError("");
        executeOutPut.setTime(0L);
        executeOutPut.setExitCode(0);
        return executeOutPut;
    }

    /**
     * 获取进程的正常输出
     * @param process
     * @return
     * @throws IOException
     */
    private static String getOutput(Process process) throws IOException {
        StrBuilder strOutPutBuilder = new StrBuilder();
        // 分批获取进程的正常输出
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            strOutPutBuilder.append(line);
        }
        return strOutPutBuilder.toString();
    }

    /**
     * 获取进程的错误输出
     *
     * @param process
     * @return
     * @throws IOException
     */
    private static String getErrorOutput(Process process) throws IOException {
        StrBuilder strErrorBuilder = new StrBuilder();
        // 分批获取进程的错误输出
        BufferedReader errorBufferReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String errorLine;
        while ((errorLine = errorBufferReader.readLine()) != null) {
            strErrorBuilder.append(errorLine);
        }
        return strErrorBuilder.toString();
    }

}
