package com.example.mybatis.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.example.mybatis.config.OssConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云 OSS 工具类
 * @author yihui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssUtil {

    private final OSS ossClient;
    private final OssConfig ossConfig;

    /**
     * 上传文件到 OSS
     *
     * @param file 上传的文件
     * @param folder 文件夹路径（如：news/images）
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // 1. 生成唯一文件名
            // 格式：folder/yyyy-MM-dd/uuid.ext
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new RuntimeException("文件名不能为空");
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
//            String  = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String fileName = folder + "/" + UUID.randomUUID() + extension;

            // 2. 设置文件元信息
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.setCacheControl("no-cache");
            metadata.setHeader("Pragma", "no-cache");
            metadata.setContentDisposition("inline;filename=" + originalFilename);

            // 3. 上传文件到 OSS
            InputStream inputStream = file.getInputStream();
            PutObjectRequest request = new PutObjectRequest(
                    ossConfig.getBucketName(),
                    fileName,
                    inputStream,
                    metadata
            );

            ossClient.putObject(request);

            log.info("文件上传成功：{}", fileName);

            // 4. 返回文件访问URL
            return generateFileUrl(fileName);

        } catch (IOException e) {
            log.error("上传文件到OSS失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成文件访问URL
     */
    private String generateFileUrl(String fileName) {
        // 如果配置了 CDN 域名，使用 CDN
        if (ossConfig.getCdnDomain() != null && !ossConfig.getCdnDomain().isEmpty()) {
            return ossConfig.getCdnDomain() + "/" + fileName;
        }

        // 否则使用 OSS 默认域名
        return "https://" + ossConfig.getBucketName() + "." +
                ossConfig.getEndpoint() + "/" + fileName;
    }

    /**
     * 删除 OSS 文件
     *
     * @param fileUrl 文件URL
     */
    public void deleteFile(String fileUrl) {
        try {
            // 从URL中提取文件key
            // 例如：https://bucket.oss-cn-hangzhou.aliyuncs.com/news/images/2024-01-15/abc.jpg
            // 提取：news/images/2024-01-15/abc.jpg
            String fileName;

            if (fileUrl.contains(".com/")) {
                fileName = fileUrl.substring(fileUrl.lastIndexOf(".com/") + 5);
            } else if (fileUrl.contains(ossConfig.getCdnDomain())) {
                fileName = fileUrl.substring(fileUrl.lastIndexOf(ossConfig.getCdnDomain()) + ossConfig.getCdnDomain().length() + 1);
            } else {
                log.warn("无法解析文件URL: {}", fileUrl);
                return;
            }

            ossClient.deleteObject(ossConfig.getBucketName(), fileName);
            log.info("删除OSS文件成功: {}", fileName);

        } catch (Exception e) {
            log.error("删除OSS文件失败: {}", fileUrl, e);
            throw new RuntimeException("删除文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查文件是否存在
     */
    public boolean doesFileExist(String fileName) {
        return ossClient.doesObjectExist(ossConfig.getBucketName(), fileName);
    }
}