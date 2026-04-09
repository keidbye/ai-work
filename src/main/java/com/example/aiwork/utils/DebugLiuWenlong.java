package com.example.aiwork.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class DebugLiuWenlong {

    public static void main(String[] args) {
        String filename = "/root/.openclaw/workspace/ai-work/考勤明细26.3.xlsx";

        List<String> restDayList = Arrays.asList(new String[] {
            "2026/03/01", "2026/03/07", "2026/03/08", "2026/03/15", "2026/03/21", "2026/03/22", "2026/03/29"
        });

        System.out.println("========== 刘文龙详细打卡记录 ==========");

        EasyExcel.read(filename, WorkVo.class, new ReadListener<WorkVo>() {
            @Override
            public void invoke(WorkVo data, AnalysisContext context) {
                if ("刘文龙".equals(data.getName())) {
                    String date = data.getDataTime();
                    String startTime = data.getStartTime();
                    String endTime = data.getEndTime();
                    String workHour = data.getWorkHourNum();
                    String vacation = data.getVacation();

                    if (date == null || date.isEmpty()) return;

                    String dateOnly = date.split(" ")[0];
                    boolean isRestDay = restDayList.contains(dateOnly);

                    System.out.println("日期: " + date + (isRestDay ? " [休息日]" : ""));
                    System.out.println("  上班时间: " + startTime + "  下班时间: " + endTime);
                    System.out.println("  工作时长: " + workHour + "  请假情况: " + vacation);

                    if (isRestDay) {
                        System.out.println("  >>> 休息日，不统计迟到 <<<");
                        System.out.println();
                        return;
                    }

                    // 判断是否迟到
                    if (startTime != null && !startTime.isEmpty()) {
                        try {
                            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
                            LocalTime time = LocalTime.parse(startTime, fmt);
                            LocalTime late9 = LocalTime.of(9, 1);
                            LocalTime late930 = LocalTime.of(9, 30);

                            if (!time.isBefore(late930)) {
                                System.out.println("  >>> 严重迟到 (09:30及以后) <<<");
                            } else if (!time.isBefore(late9)) {
                                System.out.println("  >>> 迟到 (09:01-09:30) <<<");
                            } else {
                                System.out.println("  >>> 正常 <<<");
                            }

                            // 判断加班抵消
                            if (endTime != null && endTime.contains("次日")) {
                                System.out.println("  >>> 加班到次日，次日10:00前打卡不算迟到 <<<");
                            } else if (endTime != null && !endTime.isEmpty()) {
                                try {
                                    LocalTime end = LocalTime.parse(endTime, fmt);
                                    LocalTime overtime23 = LocalTime.of(23, 0);
                                    if (!end.isBefore(overtime23)) {
                                        System.out.println("  >>> 加班到23:00以后，次日09:30前打卡不算迟到 <<<");
                                    }
                                } catch (Exception e) {
                                }
                            }

                        } catch (Exception e) {
                            System.out.println("  >>> 时间解析异常: " + startTime + " <<<");
                        }
                    } else {
                        System.out.println("  >>> 无上班打卡时间 <<<");
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
