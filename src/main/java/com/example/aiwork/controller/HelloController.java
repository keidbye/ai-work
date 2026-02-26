package com.example.aiwork.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello World!";
    }

    /**
     * 关闭程序接口
     */
    @PostMapping("/shutdown")
    public String shutdown() {
        new Thread(() -> {
            try {
                // 延迟100毫秒确保响应返回
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 退出程序
            System.exit(0);
        }).start();
        return "程序正在关闭...";
    }
}
