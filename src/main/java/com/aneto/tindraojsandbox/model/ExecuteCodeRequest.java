package com.aneto.tindraojsandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest {

    /**
     * 输入用例
     */
    private List<String> input;

    /**
     * 代码
     */
    private String code;

    /**
     * 语言
     */
    private String language;

}
