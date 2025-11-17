package com.example.mybatis.utils;

import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class FileUtils {
    public static void downloadFile(HttpServletResponse response, File file, String newFileName,boolean deleteAfter) {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                URLEncoder.encode(newFileName, StandardCharsets.UTF_8));

        try (InputStream in = new BufferedInputStream(new FileInputStream(file));
             OutputStream out = new BufferedOutputStream(response.getOutputStream())) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败", e);
        } finally {
            if (deleteAfter && file != null && file.exists()) {
                try { file.delete(); } catch (Exception ignore) {}
            }
        }
    }
}
