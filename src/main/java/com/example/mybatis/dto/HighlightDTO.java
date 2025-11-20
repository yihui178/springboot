package com.example.mybatis.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class HighlightDTO {
    private Long id;

    @NotBlank(message = "亮点名称不能为空")
    @Size(max = 20, message = "亮点名称长度不能超过20个字符")
    private String name;
}