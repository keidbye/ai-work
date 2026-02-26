package com.example.aiwork.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.net.URI;

@Component
public class BrowserOpener {

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowserAfterStartup(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String url = "http://localhost:" + port + contextPath;

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("浏览器已自动打开: " + url);
            } catch (Exception e) {
                System.err.println("无法自动打开浏览器: " + e.getMessage());
            }
        } else {
            System.err.println("当前环境不支持自动打开浏览器，请手动访问: " + url);
        }
    }
}