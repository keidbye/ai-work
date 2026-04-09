package com.example.aiwork.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DetailAttendance {

    public static void main(String[] args) {
        String filename = "/root/.openclaw/workspace/ai-work/上下班打卡_日报_20260201-20260228 (1).xlsx";

        List<String> restDayList = Arrays.asList(new String[] {
            "2026/02/01", "2026/02/08", "2026/02/15", "2026/02/16", "2026/02/17",
            "2026/02/18", "2026/02/19", "2026/02/20", "2026/02/21", "2026/02/22", "2026/02/23"
        });

        System.out.println("========== 李世鹏详细打卡记录 ==========");

        EasyExcel.read(filename, WorkVo.class, new ReadListener<WorkVo>() {
            @Override
            public void invoke(WorkVo data, AnalysisContext context) {
                if ("李世鹏".equals(data.getName())) {
                    String date = data.getDataTime();
                    String startTime = data.getStartTime();
                    String endTime = data.getEndTime();
                    String workHour = data.getWorkHourNum();
                    String vacation = data.getVacation();

                    System.out.println("日期: " + date);
                    System.out.println("  上班时间: " + startTime + "  下班时间: " + endTime);
                    System.out.println("  工作时长: " + workHour + "  请假情况: " + vacation);

                    // 判断是否迟到
                    if (startTime != null && !startTime.isEmpty()) {
                        try {
                            java.time.LocalTime time = java.time.LocalTime.parse(startTime, java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                            java.time.LocalTime late9 = java.time.LocalTime.of(9, 1);
                            java.time.LocalTime late930 = java.time.LocalTime.of(9, 30);

                            if (time.isAfter(late9) && time.isBefore(late930) || time.equals(late930)) {
                                System.out.println("  >>> 迟到 (09:01-09:30) <<<");
                            } else if (time.isAfter(late930)) {
                                System.out.println("  >>> 严重迟到 (09:30以后) <<<");
                            } else {
                                System.out.println("  >>> 正常 <<<");
                            }

                            // 判断是否加班到23点以后
                            if (endTime != null && !endTime.isEmpty() && !endTime.contains("次日")) {
                                try {
                                    java.time.LocalTime end = java.time.LocalTime.parse(endTime, java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                                    java.time.LocalTime overtime23 = java.time.LocalTime.of(23, 0);
                                    if (end.isAfter(overtime23) || end.equals(overtime23)) {
                                        System.out.println("  >>> 加班到23点以后，次日09:30前打卡不算迟到 <<<");
                                    }
                                } catch (Exception e) {
                                }
                            }
                            if (endTime != null && endTime.contains("次日")) {
                                System.out.println("  >>> 加班到次日，次日10点前打卡不算迟到 <<<");
                            }

                        } catch (Exception e) {
                            System.out.println("  >>> 时间解析异常 <<<");
                        }
                    }
                    System.out.println();
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                System.out.println("========== 分析完成 ==========");
            }
        }).sheet().headRowNumber(4).doRead();
    }
}
