package com.example.mybatis.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * @author yihui
 */
@Data
@TableName("news")
public class News {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("news_name")
    private String newsName;
    @TableField("news_content")
    private String newsContent;
    @TableField("news_category")
    private String newsCategory;
    @TableField("news_description")
    private String newsDescription;
    @TableField("has_image")
    private Boolean hasImage;
    @TableField("image_url")
    private String imageUrl;
    @TableField("news_tags")
    private String newsTags;
    @TableField("deleted")
    private Boolean deleted;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("status")
    private String status;
    @TableField("creator_id")
    private Long creatorId;
    @TableField("creator_name")
    private String creatorName;
}