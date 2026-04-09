package com.example.aiwork.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DebugAttendance {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public static void main(String[] args) {
        String filename = "/root/.openclaw/workspace/ai-work/上下班打卡_日报_20260201-20260228 (1).xlsx";

        List<String> restDayList = Arrays.asList(new String[] {
            "2026/02/01", "2026/02/08", "2026/02/15", "2026/02/16", "2026/02/17",
            "2026/02/18", "2026/02/19", "2026/02/20", "2026/02/21", "2026/02/22", "2026/02/23"
        });

        // 模拟WorkDataListener的数据结构
        Map<String, Integer> lateNumMap = new HashMap<>();
        Map<String, List<WorkVo>> lateWorkVoMap = new HashMap<>();
        lateNumMap.put("陈炎", 0);
        lateWorkVoMap.put("陈炎", new ArrayList<>());

        System.out.println("========== 陈炎迟到分析 ==========");

        EasyExcel.read(filename, WorkVo.class, new ReadListener<WorkVo>() {
            @Override
            public void invoke(WorkVo data, AnalysisContext context) {
                if ("陈炎".equals(data.getName())) {
                    String date = data.getDataTime();
                    String startTime = data.getStartTime();
                    String endTime = data.getEndTime();

                    // 跳过没有打卡时间或休息日的记录
                    if (startTime == null || startTime.isEmpty() || startTime.equals("--")) {
                        return;
                    }
                    String[] time = date.split(" ");
                    if (restDayList.contains(time[0])) {
                        return;
                    }

                    System.out.println("日期: " + date + " 上班: " + startTime + " 下班: " + endTime);

                    try {
                        LocalTime start = LocalTime.parse(startTime, formatter);

                        // 判断09:00-09:30迟到（放入lateWorkVoMap，可以被抵消）
                        if (start.isAfter(LocalTime.of(9, 0)) && start.isBefore(LocalTime.of(9, 30))) {
                            lateNumMap.put("陈炎", lateNumMap.get("陈炎") + 1);
                            lateWorkVoMap.get("陈炎").add(data);
                            System.out.println("  -> 迟到(09:01-09:30) 迟到次数+1, 加入lateWorkVoMap, 当前迟到次数=" + lateNumMap.get("陈炎"));
                        }
                        // 判断09:30以后严重迟到（只放入lateDataMap，不能被抵消）
                        else if (!start.isBefore(LocalTime.of(9, 30))) {
                            lateNumMap.put("陈炎", lateNumMap.get("陈炎") + 1);
                            System.out.println("  -> 严重迟到(09:30+) 迟到次数+1, 只放入lateDataMap, 当前迟到次数=" + lateNumMap.get("陈炎"));
                        }

                        // 判断下班时间是否超过23点，抵消次日迟到
                        if (endTime != null && !endTime.isEmpty() && !endTime.contains("次日")) {
                            try {
                                LocalTime end = LocalTime.parse(endTime, formatter);
                                if (!end.isBefore(LocalTime.of(23, 0))) {
                                    // 计算次日日期
                                    String nextDay = subtractOneDayFromDate(time[0], 1);
                                    System.out.println("  -> 加班到23点以后，查找次日(" + nextDay + ")的迟到记录...");

                                    // 查找次日是否有迟到记录
                                    List<WorkVo> workVos = lateWorkVoMap.get("陈炎");
                                    if (workVos != null) {
                                        for (WorkVo workVo : workVos) {
                                            String lateDate = workVo.getDataTime().split(" ")[0];
                                            if (lateDate.equals(nextDay)) {
                                                lateNumMap.put("陈炎", lateNumMap.get("陈炎") - 1);
                                                System.out.println("     找到次日迟到记录！迟到次数-1, 当前迟到次数=" + lateNumMap.get("陈炎"));
                                            }
                                        }
                                    }
                                    if (lateNumMap.get("陈炎") == Integer.parseInt(lateNumMap.get("陈炎").toString())) {
                                        System.out.println("     未找到次日迟到记录，无法抵消");
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (endTime != null && endTime.contains("次日")) {
                            System.out.println("  -> 加班到次日，次日10点前不算迟到");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println();
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                System.out.println("========== 总结 ==========");
                System.out.println("最终迟到次数: " + lateNumMap.get("陈炎"));
                System.out.println("lateWorkVoMap中的记录数: " + lateWorkVoMap.get("陈炎").size());
            }
        }).sheet().headRowNumber(4).doRead();
    }

    public static String subtractOneDayFromDate(String dateString, int dayNum) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        java.time.LocalDate date = java.time.LocalDate.parse(dateString, formatter);
        java.time.LocalDate previousDay = date;
        if (dayNum > 0) {
            previousDay = date.plusDays(dayNum);
        } else if (dayNum < 0) {
            previousDay = date.minusDays(-dayNum);
        }
        return previousDay.format(formatter);
    }
}
