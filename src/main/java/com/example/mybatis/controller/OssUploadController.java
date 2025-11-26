package com.example.mybatis.controller;

import com.example.mybatis.common.HttpResult;
import com.example.mybatis.common.SpringException;
import com.example.mybatis.utils.OssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/upload")
@Tag(name = "文件上传", description = "图片上传相关接口")
@RequiredArgsConstructor
public class OssUploadController {

    private final OssUtil ossUtil;

    // 允许的图片格式
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );

    // 最大文件大小：5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 上传新闻图片
     */
    @PostMapping("/news-image")
    @Operation(summary = "上传新闻图片")
    public HttpResult<Map<String, String>> uploadNewsImage(@RequestParam("file") MultipartFile file) {
        log.info("收到图片上传请求，文件名: {}, 大小: {} bytes",
                file.getOriginalFilename(), file.getSize());

        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new SpringException("请选择要上传的文件", 400);
        }

        // 2. 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new SpringException("文件大小不能超过5MB", 400);
        }

        // 3. 校验文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new SpringException("文件名不能为空", 400);
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new SpringException("只支持 JPG、PNG、GIF、WEBP、BMP 格式的图片", 400);
        }

        // 4. 校验文件内容类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new SpringException("只能上传图片文件", 400);
        }

        // 5. 上传到 OSS
        try {
            String imageUrl = ossUtil.uploadFile(file, "HBBTJ/news/images");

            log.info("图片上传成功: {}", imageUrl);

            Map<String, String> result = new HashMap<>();
            result.put("url", imageUrl);
            result.put("name", originalFilename);
            result.put("size", String.valueOf(file.getSize()));

            return HttpResult.ok(result);

        } catch (Exception e) {
            log.error("上传图片失败", e);
            throw new SpringException("上传失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 删除图片
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除图片")
    public HttpResult<String> deleteImage(@RequestParam String url) {
        log.info("收到删除图片请求: {}", url);

        if (url == null || url.isEmpty()) {
            throw new SpringException("图片URL不能为空", 400);
        }

        try {
            ossUtil.deleteFile(url);
            log.info("图片删除成功: {}", url);
            return HttpResult.ok("删除成功");

        } catch (Exception e) {
            log.error("删除图片失败: {}", url, e);
            throw new SpringException("删除失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}