package com.example.mybatis.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mybatis.common.FileUtils;
import com.example.mybatis.common.PoiUtils;
import com.example.mybatis.entity.User;
import com.example.mybatis.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;




@RestController
public class UserExcelController {

    private final UserService userService;

    public UserExcelController(UserService userService) {
        this.userService = userService;
    }

    @Tag(name = "Excel导出接口", description = "提供excel下载")
    @Operation(summary = "提供excel下载")
    @PostMapping(value = "/exportExcelUser")
    public void exportExcelUser(HttpServletResponse res) {
        // 使用 User 类而非 SysUser
        List<User> records = userService.list(); // 获取所有用户数据
        Workbook workbook = new XSSFWorkbook(); // 创建一个新的 Excel 工作簿
        Sheet sheet = workbook.createSheet(); // 创建一个新的工作表
        Row row0 = sheet.createRow(0); // 创建表头

        // 创建表头
        int columnIndex = 0;
        row0.createCell(columnIndex).setCellValue("No");
        row0.createCell(++columnIndex).setCellValue("ID");
        row0.createCell(++columnIndex).setCellValue("昵称");
        row0.createCell(++columnIndex).setCellValue("年龄");
        row0.createCell(++columnIndex).setCellValue("邮箱");
//        row0.createCell(++columnIndex).setCellValue("手机号");

        // 填充数据
        for (int i = 0; i < records.size(); i++) {
            User user = records.get(i); // 使用 User 对象
            Row row = sheet.createRow(i + 1); // 创建每一行数据
            columnIndex = 0;
            row.createCell(columnIndex).setCellValue(i + 1); // 设置序号
            row.createCell(++columnIndex).setCellValue(user.getId());
            row.createCell(++columnIndex).setCellValue(user.getName());
            row.createCell(++columnIndex).setCellValue(user.getAge());
            row.createCell(++columnIndex).setCellValue(user.getEmail());
//            row.createCell(++columnIndex).setCellValue(user.getPhone());
        }

        // 创建文件并下载
        File file = PoiUtils.createExcelFile(workbook, "download_user");
        FileUtils.downloadFile(res, file, file.getName());
    }

    @Tag(name = "Excel导出接口", description = "提供excel下载")
    @Operation(summary = "提供EasyExcel下载")
    @GetMapping("/exportUsers")
    public void exportUsers(HttpServletResponse response) throws IOException {
        // 设置响应头
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");

        String fileName = URLEncoder.encode("用户信息导出", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

//        // 从数据库获取数据
//        List<User> userList = userService.list();
//
//        // 导出到浏览器
//        EasyExcel.write(response.getOutputStream(), User.class)
//                .sheet("用户信息")
//                .doWrite(userList);
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), User.class).build();
        WriteSheet writeSheet = EasyExcel.writerSheet("用户数据").build();

        // 分页查询 + 写入循环
        int page = 1;
        int pageSize = 5000;
        Wrapper<User> queryWrapper = new QueryWrapper<>();  // 你可以添加条件 queryWrapper.eq("status", 1);
        while (true) {
            IPage<User> iPage = new Page<>(page, pageSize);
            IPage<User> resultPage = userService.page(iPage, queryWrapper);
            List<User> list = resultPage.getRecords();
            if (list.isEmpty()) break;

            excelWriter.write(list, writeSheet);
            if (list.size() < pageSize) break; // 最后一页
            page++;
        }

        excelWriter.finish();
    }
}
