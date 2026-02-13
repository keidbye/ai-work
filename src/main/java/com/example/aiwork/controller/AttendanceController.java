package com.example.aiwork.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.example.aiwork.utils.WorkDataListener;
import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".xlsx";
            Path tempFile = Files.createTempFile(Path.of(UPLOAD_DIR), "upload_", extension);
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            List<String> restDayList = parseRestDays(restDays);
            List<String> specialNameList = parseSpecialNames(specialNames);

            List<Map<String, Object>> counts = WorkDataListener.getCounts(
                    tempFile.toString(),
                    restDayList,
                    specialNameList
            );

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

    @PostMapping("/export")
    public void export(
            HttpServletResponse response,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "restDays", required = false) String restDays,
            @RequestParam(value = "specialNames", required = false) String specialNames) {

        try {
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".xlsx";
            Path tempFile = Files.createTempFile(Path.of(UPLOAD_DIR), "upload_", extension);
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            List<String> restDayList = parseRestDays(restDays);
            List<String> specialNameList = parseSpecialNames(specialNames);

            List<Map<String, Object>> counts = WorkDataListener.getCounts(
                    tempFile.toString(),
                    restDayList,
                    specialNameList
            );

            Files.deleteIfExists(tempFile);

            System.err.println("Export data count: " + counts.size());

            List<ExportData> exportDataList = new ArrayList<>();
            for (Map<String, Object> item : counts) {
                ExportData data = new ExportData();
                data.setName((String) item.get("name"));
                data.setDayNum(getIntValue(item.get("dayNum")));
                data.setHourNum(getIntValue(item.get("hourNum")));
                data.setLeaveNum(getIntValue(item.get("leaveNum")));
                data.setNoCheckInNum(getIntValue(item.get("noCheckInNum")));
                data.setLateNum(getIntValue(item.get("lateNum")));
                data.setRestDayWordNum(getFloatValue(item.get("restDayWordNum")));
                data.setSubsidyNum(getIntValue(item.get("subsidyNum")));
                data.setHour19To21Num(getIntValue(item.get("hour19To21Num")));
                data.setHour21To05Num(getIntValue(item.get("hour21To05Num")));
                exportDataList.add(data);

                System.err.println("Export: " + data.getName() + " - dayNum:" + data.getDayNum());
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("考勤统计结果", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            EasyExcel.write(response.getOutputStream(), ExportData.class)
                    .sheet("考勤统计")
                    .doWrite(exportDataList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getIntValue(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof AtomicInteger) return ((AtomicInteger) value).get();
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private float getFloatValue(Object value) {
        if (value == null) return 0f;
        if (value instanceof Float) return (Float) value;
        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return 0f;
        }
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

    @Data
    public static class ExportData {
        @ExcelProperty("姓名")
        @ColumnWidth(12)
        private String name;

        @ExcelProperty("天数")
        private Integer dayNum;

        @ExcelProperty("小时数")
        private Integer hourNum;

        @ExcelProperty("请假数")
        private Integer leaveNum;

        @ExcelProperty("未打卡数")
        private Integer noCheckInNum;

        @ExcelProperty("迟到天数")
        private Integer lateNum;

        @ExcelProperty("周末小时")
        private Float restDayWordNum;

        @ExcelProperty("周末补贴次数")
        private Integer subsidyNum;

        @ExcelProperty("19-21加班")
        private Integer hour19To21Num;

        @ExcelProperty("21-05加班")
        private Integer hour21To05Num;
    }
}
