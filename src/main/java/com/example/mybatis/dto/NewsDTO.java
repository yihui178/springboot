package com.example.mybatis.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
/**
 * @author yihui
 */
@Data
@Schema(description = "新闻数据传输对象")
public class NewsDTO {
    @Schema(description = "新闻ID")
    private Long id;
    @NotBlank(message = "新闻名称不能为空")
    @Size(max = 20, message = "新闻名称不能超过20个字")
    @Schema(description = "新闻名称")
    private String newsName;
    @NotBlank(message = "新闻内容不能为空")
    @Size(max = 200, message = "新闻内容不能超过200个字")
    @Schema(description = "新闻内容")
    private String newsContent;
    @NotEmpty(message = "新闻分类不能为空")
    @Schema(description = "新闻分类（多选）")
    private List<String> newsCategory;
    @NotBlank(message = "新闻简介不能为空")
    @Schema(description = "新闻简介")
    private String newsDescription;
    @NotNull(message = "是否有配图不能为空")
    @Schema(description = "是否有配图")
    private Boolean hasImage;
    @Schema(description = "图片URL")
    private String imageUrl;
    @NotEmpty(message = "新闻标签不能为空")
    @Schema(description = "新闻标签（多选）")
    private List<String> newsTags;
    @Schema(description = "是否已删除")
    private Boolean deleted;
}