package com.example.aiwork.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestChenYan {
    public static void main(String[] args) {
        String filename = "/root/.openclaw/workspace/ai-work/上下班打卡_日报_20260201-20260228 (1).xlsx";

        List<String> restDayList = Arrays.asList(new String[] {
            "2026/02/01", "2026/02/08", "2026/02/15", "2026/02/16", "2026/02/17",
            "2026/02/18", "2026/02/19", "2026/02/20", "2026/02/21", "2026/02/22", "2026/02/23"
        });

        List<String> filterNameList = Arrays.asList(new String[] {
            "顾良聪"
        });

        List<Map<String, Object>> counts = WorkDataListener.getCounts(filename, restDayList, filterNameList);

        for (Map<String, Object> map : counts) {
            if ("陈炎".equals(map.get("name"))) {
                System.out.println("========== 陈炎考勤结果 ==========");
                System.out.println("姓名: " + map.get("name"));
                System.out.println("迟到次数: " + map.get("lateNum"));
                System.out.println("未打卡次数: " + map.get("noCheckInNum"));
                System.out.println("请假次数: " + map.get("leaveNum"));
                System.out.println("正常打卡天数: " + map.get("dayNum"));
                System.out.println("加班小时数: " + map.get("hourNum"));
            }
        }
    }
}
