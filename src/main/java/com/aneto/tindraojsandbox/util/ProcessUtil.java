package com.aneto.tindraojsandbox.util;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.text.StrBuilder;
import com.aneto.tindraojsandbox.model.ExecuteOutPut;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessUtil {

    public static ExecuteOutPut execute(Process process, String operation) throws InterruptedException, IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ExecuteOutPut executeOutPut = new ExecuteOutPut();
        // 等待编译完成
        int exitValue = process.waitFor();
        executeOutPut.setExitCode(exitValue);
        if (exitValue != 0) {
            System.out.println(operation + "失败" + exitValue);
            // 分批获取进程的正常输出
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StrBuilder strOutPutBuilder = new StrBuilder();
            // 逐行读取
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                strOutPutBuilder.append(line).append("\n");
            }
            // 分批获取进程的错误输出
            BufferedReader errorBufferReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StrBuilder strErrorBuilder = new StrBuilder();
            // 逐行读取
            String errorLine;
            while ((errorLine = errorBufferReader.readLine()) != null) {
                strErrorBuilder.append(errorLine).append("\n");
            }
            executeOutPut.setError(strErrorBuilder.toString());
        } else {
            System.out.println(operation + "成功");

            // 分批获取进程的正常输出
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StrBuilder strOutPutBuilder = new StrBuilder();
            // 逐行读取
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                strOutPutBuilder.append(line).append("\n");
            }
            executeOutPut.setOutPut(strOutPutBuilder.toString());
        }
        stopWatch.stop();
        executeOutPut.setTime(stopWatch.getTotalTimeMillis());
        return executeOutPut;
    }
}
