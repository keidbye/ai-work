package com.example.aiwork.controller;

import com.example.aiwork.utils.WorkDataListener;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AttendanceController {

    private static final String UPLOAD_DIR = "temp-uploads";

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/calculate")
    @ResponseBody
    public Map<String, Object> calculate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "restDays", required = false) String restDays,
            @RequestParam(value = "specialNames", required = false) String specialNames) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 创建上传目录
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 保存上传的文件
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".xlsx";
            Path tempFile = Files.createTempFile(Path.of(UPLOAD_DIR), "upload_", extension);
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            // 解析参数
            List<String> restDayList = parseRestDays(restDays);
            List<String> specialNameList = parseSpecialNames(specialNames);

            // 执行计算
            List<Map<String, Object>> counts = WorkDataListener.getCounts(
                    tempFile.toString(),
                    restDayList,
                    specialNameList
            );

            // 删除临时文件
            Files.deleteIfExists(tempFile);

            result.put("success", true);
            result.put("data", counts);
            result.put("message", "计算成功");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private List<String> parseRestDays(String restDays) {
        if (restDays == null || restDays.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(restDays.split("[,\\s\\n]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> parseSpecialNames(String specialNames) {
        if (specialNames == null || specialNames.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(specialNames.split("[,，\\s\\n]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
