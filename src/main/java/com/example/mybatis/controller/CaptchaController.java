//package com.example.mybatis.controller;
//
//import com.anji.captcha.model.common.ResponseModel;
//import com.anji.captcha.model.vo.CaptchaVO;
//import com.anji.captcha.service.CaptchaService;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//
///**
// * 生成和校验行为验证码（滑块 / 点选验证码）。
// * @author yihui
// */
//@RestController
//@RequestMapping("/captcha")
//@RequiredArgsConstructor
//public class CaptchaController {
//
//    private final CaptchaService captchaService;
//
//    @PostMapping("/get")
//    public ResponseModel get(@RequestBody CaptchaVO data, HttpServletRequest request) {
//        data.setBrowserInfo(request.getRemoteAddr() + request.getHeader("user-agent"));
//        // 若前端未指定类型，默认使用滑块
//        if (data.getCaptchaType() == null) {
//            data.setCaptchaType("blockPuzzle");
//        }
//
//        return captchaService.get(data);
//    }
//
//    @PostMapping("/check")
//    public ResponseModel check(@RequestBody CaptchaVO data, HttpServletRequest request) {
//        data.setBrowserInfo(request.getRemoteAddr() + request.getHeader("user-agent"));
//        return captchaService.check(data);
//    }
//}
