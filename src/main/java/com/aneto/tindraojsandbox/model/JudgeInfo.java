package com.aneto.tindraojsandbox.model;

import lombok.Data;

/**
 * 判题信息（内存、时间、执行信息）
 */
@Data
public class JudgeInfo {

    /**
     * 代码执行信息
     */
    private String message;
    /**
     * 执行消耗内存
     */
    private Long memory;
    /**
     * 执行消耗时间
     */
    private Long time;
}
