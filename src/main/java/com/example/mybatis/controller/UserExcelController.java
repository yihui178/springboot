package com.example.mybatis.controller;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mybatis.service.RoleService;
import com.example.mybatis.service.UserRoleService;
import com.example.mybatis.utils.FileUtils;
import com.example.mybatis.utils.PoiUtils;
import com.example.mybatis.entity.User;
import com.example.mybatis.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * 用户 Excel 导入导出控制器
 *
 * @author yihui
 */
@Slf4j
@RestController
@Tag(name = "Excel 导入导出", description = "用户数据的 Excel 导入导出接口")
@RequiredArgsConstructor
public class UserExcelController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleService userRoleService;
    private final RoleService roleService;
    /**
     * 导出用户数据（POI 方式）
     */
    @PostMapping("/exportExcelUser")
    @Operation(summary = "导出用户数据（POI）")
    public void exportExcelUser(HttpServletResponse response) {
        // 使用局部变量
        List<User> records = userService.list();
        // try-with-resources
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("用户数据");

            // 创建表头
            createHeader(sheet);

            // 填充数据
            fillData(sheet, records);

            // 创建文件并下载
            File file = PoiUtils.createExcelFile(workbook, "download_user");
            FileUtils.downloadFile(response, file, file.getName(), true);

        } catch (Exception e) {
            log.error("导出用户数据失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }
    /**
     * 导出用户数据（EasyExcel 方式）
     */
    @GetMapping("/exportUsers")
    @Operation(summary = "导出用户数据（EasyExcel）")
    public void exportUsers(HttpServletResponse response) {
        try {
            // 设置响应头
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            String fileName = URLEncoder.encode("用户信息导出", StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
            // 使用 try-with-resources
            try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), User.class).build()) {
                exportUserData(excelWriter);
            }

        } catch (IOException e) {
            log.error("导出用户数据失败", e);
            writeErrorResponse(response);
        }
    }
    /**
     * 导入用户数据（异步）
     */
    @PostMapping(value = "/importUsers", consumes = "multipart/form-data")
    @Operation(summary = "异步导入用户数据")
    public String importUsers(@RequestPart("file") MultipartFile file) {
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        CompletableFuture.runAsync(() -> {
            try {
                EasyExcel.read(file.getInputStream(), User.class,
                                new UserExcelListener(userService, passwordEncoder, userRoleService, roleService))
                        .sheet()
                        .doRead();
            } catch (IOException e) {
                log.error("导入用户数据失败", e);
            }
        }, executorService);

        // 记得关闭线程池（或使用全局线程池）
        executorService.shutdown();

        return "导入任务已开始，请稍后查看结果。";
    }
    // ==================== 私有辅助方法 ====================
    /**
     * 创建 Excel 表头
     */
    private void createHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        int columnIndex = 0;
        headerRow.createCell(columnIndex++).setCellValue("序号");
        headerRow.createCell(columnIndex++).setCellValue("ID");
        headerRow.createCell(columnIndex++).setCellValue("姓名");
        headerRow.createCell(columnIndex++).setCellValue("密码");
        headerRow.createCell(columnIndex++).setCellValue("年龄");
        headerRow.createCell(columnIndex).setCellValue("邮箱");
    }
    /**
     * 填充 Excel 数据
     */
    private void fillData(Sheet sheet, List<User> records) {
        for (int i = 0; i < records.size(); i++) {
            User user = records.get(i);
            Row row = sheet.createRow(i + 1);
            int columnIndex = 0;

            row.createCell(columnIndex++).setCellValue(i + 1);
            row.createCell(columnIndex++).setCellValue(user.getId());
            row.createCell(columnIndex++).setCellValue(user.getName());
            row.createCell(columnIndex++).setCellValue(user.getPassword());
            row.createCell(columnIndex++).setCellValue(user.getAge());
            row.createCell(columnIndex).setCellValue(user.getEmail());
        }
    }
    /**
     * 导出用户数据（分页导出）
     */
    private void exportUserData(ExcelWriter excelWriter) {
        int currentPage = 1;
        // 每个 sheet 最多 50 万行
        int pageSize = 500000;
        int sheetNo = 0;
        while (true) {
            IPage<User> iPage = new Page<>(currentPage, pageSize);
            List<User> list = userService.page(iPage).getRecords();
            if (list.isEmpty()) {
                break;
            }
            // 写入数据到 sheet
            WriteSheet writeSheet = EasyExcel.writerSheet(sheetNo, "用户数据" + (sheetNo + 1)).build();
            excelWriter.write(list, writeSheet);
            // 如果数据量小于一页，说明已经写完
            if (list.size() < pageSize) {
                break;
            }
            currentPage++;
            sheetNo++;
        }
    }
    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response) {
        try {
            response.reset();
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"message\":\"" + "导出失败，请联系管理员！" + "\"}");
        } catch (IOException ex) {
            log.error("写入错误响应失败", ex);
        }
    }
}