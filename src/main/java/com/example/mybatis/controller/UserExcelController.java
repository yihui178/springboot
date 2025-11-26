package com.example.mybatis.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mybatis.utils.FileUtils;
import com.example.mybatis.utils.PoiUtils;
import com.example.mybatis.entity.User;
import com.example.mybatis.mapper.UserMapper;
import com.example.mybatis.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author yihui
 */
@RestController
public class UserExcelController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    //  构造器注入（多个参数）
    public UserExcelController(
            UserService userService,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }
    @Tag(name = "Excel导出接口", description = "提供excel下载")
    @Operation(summary = "提供excel下载")
    @PostMapping(value = "/exportExcelUser")
    public void exportExcelUser(HttpServletResponse res) {
        // 使用 User 类而非 SysUser
        // 获取所有用户数据
        List<User> records = userService.list();
        // 创建一个新的 Excel 工作簿
        Workbook workbook = new XSSFWorkbook();
        // 创建一个新的工作表
        Sheet sheet = workbook.createSheet();
        // 创建表头
        Row row0 = sheet.createRow(0);

        // 创建表头
        int columnIndex = 0;
        row0.createCell(columnIndex).setCellValue("No");
        row0.createCell(++columnIndex).setCellValue("ID");
        row0.createCell(++columnIndex).setCellValue("姓名");
        row0.createCell(++columnIndex).setCellValue("密码");
        row0.createCell(++columnIndex).setCellValue("年龄");
        row0.createCell(++columnIndex).setCellValue("邮箱");
//        row0.createCell(++columnIndex).setCellValue("手机号");

        // 填充数据
        for (int i = 0; i < records.size(); i++) {
            // 使用 User 对象
            User user = records.get(i);
            // 创建每一行数据
            Row row = sheet.createRow(i + 1);
            columnIndex = 0;
            // 设置序号
            row.createCell(columnIndex).setCellValue(i + 1);
            row.createCell(++columnIndex).setCellValue(user.getId());
            row.createCell(++columnIndex).setCellValue(user.getName());
            row.createCell(++columnIndex).setCellValue(user.getPassword());
            row.createCell(++columnIndex).setCellValue(user.getAge());
            row.createCell(++columnIndex).setCellValue(user.getEmail());
//            row.createCell(++columnIndex).setCellValue(user.getPhone());
        }

        // 创建文件并下载
        File file = PoiUtils.createExcelFile(workbook, "download_user");
        FileUtils.downloadFile(res, file, file.getName(), true);
    }

    @Tag(name = "Excel导出接口", description = "提供excel下载")
    @Operation(summary = "提供EasyExcel下载")
    @GetMapping("/exportUsers")
    public void exportUsers(HttpServletResponse response) {
        ExcelWriter excelWriter = null;
        try {
            // 设置响应头
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("用户信息导出", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

            // 构建 EasyExcel 写入器
            excelWriter = EasyExcel.write(response.getOutputStream(), User.class).build();

            int page = 1;
            // 每个sheet最多写50万行，防止超出上限
            int pageSize = 500000;
            int sheetNo = 0;

            while (true) {
                IPage<User> iPage = new Page<>(page, pageSize);
                List<User> list = userService.page(iPage).getRecords();

                if (list == null || list.isEmpty()) {
                    break;
                }

                // 每次新建一个sheet
                WriteSheet writeSheet = EasyExcel.writerSheet(sheetNo, "用户数据" + (sheetNo + 1)).build();
                excelWriter.write(list, writeSheet);

                // 小于一页说明写完
                if (list.size() < pageSize) {
                    break;
                }

                page++;
                sheetNo++;
            }

            excelWriter.finish();
        } catch (Exception e) {
            // 如果出现异常，返回JSON提示
            try {
                response.reset();
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"message\":\"导出失败，请联系管理员！\"}");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }




    @Tag(name = "Excel导入接口", description = "异步导入用户数据")
    @Operation(summary = "异步上传 Excel 文件导入用户")
    @PostMapping(value = "/importUsers", consumes = "multipart/form-data")
    public String importUsers(
            @Parameter(description = "Excel 文件", required = true)
            @RequestPart("file") MultipartFile file) throws Exception {

        //  第1步：重置自增主键
        Long maxId = userService.lambdaQuery()
                .select(User::getId)
                .orderByDesc(User::getId)
                .last("LIMIT 1")
                .oneOpt()
                .map(User::getId)
                .orElse(0L);
        userMapper.updateAutoIncrement(maxId + 1);

        //  第2步：异步执行导入任务
        ExecutorService executorService = Executors.newFixedThreadPool(4); // 可调整线程数
        CompletableFuture.runAsync(() -> {
            try {
                EasyExcel.read(file.getInputStream(), User.class, new UserExcelListener(userService,passwordEncoder))
                        .sheet()
                        .doRead();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executorService);

        // 不等待导入完成，直接返回响应
        return "导入任务已开始，请稍后查看结果。";
    }















}
