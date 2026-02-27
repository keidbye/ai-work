package com.example.aiwork;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiWorkApplication {

    private static final int PORT = 9394;

    public static void main(String[] args) {
        // 启动前先杀掉占用端口的进程
        killProcessOnPort(PORT);
        SpringApplication.run(AiWorkApplication.class, args);
    }

    /**
     * 杀掉占用指定端口的进程
     */
    private static void killProcessOnPort(int port) {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            Process process;
            ProcessBuilder processBuilder;

            if (osName.contains("win")) {
                // Windows 系统
                // 查找占用端口的进程
                processBuilder = new ProcessBuilder("cmd", "/c",
                        "netstat -ano | findstr :" + port);
                processBuilder.redirectErrorStream(true);
                process = processBuilder.start();

                String output = new String(process.getInputStream().readAllBytes());
                process.waitFor();

                // 解析输出获取 PID
                String[] lines = output.split("\n");
                for (String line : lines) {
                    if (line.contains("LISTENING")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 5) {
                            String pid = parts[parts.length - 1];
                            // 杀掉进程
                            Runtime.getRuntime().exec("taskkill /F /PID " + pid);
                            System.out.println("已杀掉占用端口 " + port + " 的进程，PID: " + pid);
                        }
                    }
                }
            } else if (osName.contains("mac") || osName.contains("nix") || osName.contains("nux")) {
                // Linux/Mac 系统
                processBuilder = new ProcessBuilder("sh", "-c",
                        "lsof -ti:" + port);
                processBuilder.redirectErrorStream(true);
                process = processBuilder.start();

                String output = new String(process.getInputStream().readAllBytes());
                process.waitFor();

                if (!output.trim().isEmpty()) {
                    String pid = output.trim();
                    Runtime.getRuntime().exec("kill -9 " + pid);
                    System.out.println("已杀掉占用端口 " + port + " 的进程，PID: " + pid);
                }
            }
        } catch (Exception e) {
            // 忽略错误，继续启动
            System.out.println("检查端口占用时出错: " + e.getMessage());
        }
    }
}
