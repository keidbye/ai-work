package com.example.aiwork;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.net.URI;

@SpringBootApplication
public class AiWorkApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiWorkApplication.class, args);
    }

    @Component
    public static class BrowserOpener {
        private String url = "http://localhost:8080";

        @PostConstruct
        public void openBrowser() {
            // 延迟1秒，等待应用完全启动
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI(url));
                    }
                } catch (Exception e) {
                    // 记录日志，不影响主程序
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
