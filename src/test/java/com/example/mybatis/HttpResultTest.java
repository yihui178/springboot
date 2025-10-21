package com.example.mybatis;

import com.example.mybatis.http.HttpResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HttpResultTest {

    @Test
    public void testOk() {
        HttpResult result = HttpResult.ok("操作成功");
        System.out.println(result.getCode());  // 输出状态码
        System.out.println(result.getMsg());   // 输出消息
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMsg());
    }

//    @Test
//    public void testError() {
//        HttpResult result = HttpResult.error("发生错误");
//        System.out.println(result.getCode());  // 输出状态码
//        System.out.println(result.getMsg());   // 输出消息
//        assertEquals(500, result.getCode());
//        assertEquals("发生错误", result.getMsg());
//    }

}
