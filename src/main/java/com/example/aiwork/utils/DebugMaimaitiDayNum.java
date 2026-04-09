package com.example.aiwork.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;

import java.util.Arrays;
import java.util.List;

public class DebugMaimaitiDayNum {
    public static void main(String[] args) {
        String filename = "/root/.openclaw/workspace/ai-work/上下班打卡_日报_20260201-20260228 (1).xlsx";

        List<String> restDayList = Arrays.asList(new String[] {
            "2026/02/01", "2026/02/08", "2026/02/15", "2026/02/16", "2026/02/17",
            "2026/02/18", "2026/02/19", "2026/02/20", "2026/02/21", "2026/02/22", "2026/02/23"
        });

        System.out.println("========== 买买提加班天数统计 ==========");

        final int[] dayNum = {0};

        EasyExcel.read(filename, WorkVo.class, new ReadListener<WorkVo>() {
            @Override
            public void invoke(WorkVo data, AnalysisContext context) {
                if ("买买提".equals(data.getName())) {
                    String date = data.getDataTime();
                    String endTime = data.getEndTime();

                    // 跳过没有打卡时间
                    if (endTime == null || endTime.isEmpty() || endTime.equals("--")) {
                        return;
                    }
                    String[] time = date.split(" ");
                    if (restDayList.contains(time[0])) {
                        return;
                    }
                    if (date.contains("星期六") || date.contains("星期日")) {
                        return;
                    }

                    // 模拟代码逻辑：次日设为23:59
                    String calcEndTime = endTime;
                    if (endTime.contains("次日")) {
                        calcEndTime = "23:59";
                        System.out.print("日期: " + date + " | 下班: " + endTime + " -> 23:59");
                    } else {
                        System.out.print("日期: " + date + " | 下班: " + endTime);
                    }

                    // 计算加班小时数
                    try {
                        long hours = WorkDataListener.calculateHoursDifference(calcEndTime, "19:00");
                        if (hours >= 2) {
                            dayNum[0]++;
                            System.out.println(" | 加班" + hours + "小时 | 天数+1 (累计: " + dayNum[0] + ")");
                        } else {
                            System.out.println(" | 加班" + hours + "小时 | 天数不+1");
                        }
                    } catch (Exception e) {
                        System.out.println(" | 计算出错: " + e.getMessage());
                    }
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                System.out.println("========== 总结 ==========");
                System.out.println("总加班天数: " + dayNum[0]);
                System.out.println("预期总天数: 11");
            }
        }).sheet().headRowNumber(4).doRead();
    }
}
