package com.example.mybatis.utils;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PoiUtils {
    public static File createExcelFile(Workbook workbook, String fileName) {
        OutputStream stream = null;
        File file = null;
        try {
            file = File.createTempFile(fileName, ".xlsx"); // 创建临时 Excel 文件
            stream = new FileOutputStream(file.getAbsoluteFile());
            workbook.write(stream); // 将数据写入文件
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(workbook);
            IOUtils.closeQuietly(stream);
        }
        return file;
    }
}
