package com.aneto.tindraojsandbox.model;

import lombok.Data;

/**
 * 代码执行控制台输出信息
 */
@Data
public class ExecuteOutPut {

    private Integer exitCode;

    private String outPut;

    private String error;

    private Long time;

    private Long memory;
}
