package com.example.aiwork.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class BrowserOpener {

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowserAfterStartup(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String port = env.getProperty("server.port", "9394");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String url = "http://localhost:" + port + contextPath;

        String osName = System.getProperty("os.name").toLowerCase();
        Runtime runtime = Runtime.getRuntime();

        try {
            if (osName.contains("win")) {
                // Windows 使用 cmd 命令打开
                runtime.exec(new String[]{"cmd", "/c", "start", url});
                System.out.println("浏览器已自动打开: " + url);
            } else if (osName.contains("mac")) {
                // macOS 使用 open 命令
                runtime.exec(new String[]{"open", url});
                System.out.println("浏览器已自动打开: " + url);
            } else if (osName.contains("nix") || osName.contains("nux")) {
                // Linux 使用 xdg-open 命令
                runtime.exec(new String[]{"xdg-open", url});
                System.out.println("浏览器已自动打开: " + url);
            } else {
                System.err.println("未知操作系统，请手动访问: " + url);
            }
        } catch (Exception e) {
            System.err.println("无法自动打开浏览器: " + e.getMessage());
            System.err.println("请手动访问: " + url);
        }
    }
}