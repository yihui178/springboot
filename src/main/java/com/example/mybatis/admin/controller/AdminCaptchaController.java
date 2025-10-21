package com.example.mybatis.admin.controller;

import com.google.code.kaptcha.Producer;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@RestController
public class AdminCaptchaController {
    @Autowired
    private Producer captchaProducer;

    @ApiOperation(value = "Get Captcha", notes = "Generate a captcha image. Access at /captcha.jpg")
    @GetMapping("/captcha.jpg")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 生成验证码字符串
        String captchaText = captchaProducer.createText();
        // 将验证码字符串保存到 session 中
        request.getSession().setAttribute("captcha", captchaText);

        // 生成验证码图片
        BufferedImage captchaImage = captchaProducer.createImage(captchaText);

        // 设置响应类型为图片
        response.setContentType("image/jpeg");

        // 使用 ImageIO 将图像写入输出流
        ImageIO.write(captchaImage, "JPEG", response.getOutputStream());
    }
}
