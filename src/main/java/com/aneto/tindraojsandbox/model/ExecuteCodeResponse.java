package com.aneto.tindraojsandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeResponse {

    /**
     * 实际输出结果
     */
    private List<String> output;

    /**
     * 沙箱状态信息
     */
    private String message;

    /**
     * 代码实际运行时间,内存消耗
     */
    private JudgeInfo judgeInfo;

    /**
     * 代码执行状态
     * */
    private Integer status;
}
