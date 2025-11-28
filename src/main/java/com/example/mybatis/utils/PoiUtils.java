package com.example.mybatis.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author yihui
 */
@Slf4j
public class PoiUtils {
    public static File createExcelFile(Workbook workbook, String fileName) {
        OutputStream stream = null;
        File file = null;
        try {
            // 创建临时 Excel 文件
            file = File.createTempFile(fileName, ".xlsx");
            stream = new FileOutputStream(file.getAbsoluteFile());
            // 将数据写入文件
            workbook.write(stream);
            log.info("Excel文件创建成功: {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("创建Excel文件失败, fileName: {}", fileName, e);
            return null;
        } finally {
            IOUtils.closeQuietly(workbook);
            IOUtils.closeQuietly(stream);
        }
        return file;
    }
}
