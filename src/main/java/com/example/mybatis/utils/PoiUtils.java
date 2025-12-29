package com.example.mybatis.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * POI 工具类
 * @author yihui
 */
@Slf4j
public class PoiUtils {

    public static File createExcelFile(Workbook workbook, String fileName) {
        try {
            // 创建临时 Excel 文件
            File file = File.createTempFile(fileName, ".xlsx");

            // 使用 try-with-resources 自动关闭资源
            try (FileOutputStream stream = new FileOutputStream(file);
                 Workbook wb = workbook) {
                wb.write(stream);
                log.info("Excel文件创建成功: {}", file.getAbsolutePath());
                return file;
            }
        } catch (IOException e) {
            log.error("创建Excel文件失败, fileName: {}", fileName, e);
            return null;
        }
    }
}