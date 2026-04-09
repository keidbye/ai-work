package com.example.aiwork.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestSpecialOvertime {
    public static void main(String[] args) {
        String filename = "/root/.openclaw/workspace/ai-work/上下班打卡_日报_20260201-20260228 (1).xlsx";

        // 设置休息日列表
        List<String> restDayList = Arrays.asList(new String[] {
            "2026/02/01", "2026/02/08", "2026/02/15", "2026/02/16", "2026/02/17",
            "2026/02/18", "2026/02/19", "2026/02/20", "2026/02/21", "2026/02/22", "2026/02/23"
        });

        // 测试特殊规则员工（顾良聪）
        List<String> filterNameList = Arrays.asList(new String[] {
            "顾良聪"
        });

        System.out.println("========== 特殊员工加班统计测试 ==========");
        List<Map<String, Object>> counts = WorkDataListener.getCounts(filename, restDayList, filterNameList);

        for (Map<String, Object> map : counts) {
            if ("顾良聪".equals(map.get("name"))) {
                System.out.println("顾良聪考勤统计：");
                System.out.println("- 19:00-21:00加班小时：" + map.get("hour19To21Num"));
                System.out.println("- 21:00-05:00加班小时：" + map.get("hour21To05Num"));

                // 测试convertMinutesToHours方法
                System.out.println("\n========== convertMinutesToHours方法测试 ==========");
                testConvertMinutesToHours();
                break;
            }
        }
    }

    private static void testConvertMinutesToHours() {
        // 创建一个测试实例
        WorkDataListener listener = new WorkDataListener();

        // 测试用反射调用私有方法
        try {
            java.lang.reflect.Method method = WorkDataListener.class.getDeclaredMethod("convertMinutesToHours", long.class);
            method.setAccessible(true);

            // 测试各种分钟数
            float[][] testCases = {
                {0, 0},      // 0分钟 = 0小时
                {10, 0},     // 10分钟 = 0小时
                {20, 0},     // 20分钟 = 0小时
                {21, 0.5f},  // 21分钟 = 0.5小时
                {30, 0.5f},  // 30分钟 = 0.5小时
                {50, 0.5f},  // 50分钟 = 0.5小时
                {51, 1},     // 51分钟 = 1小时
                {60, 1},     // 60分钟 = 1小时
                {70, 1},     // 70分钟 = 1小时
                {80, 1.5f},  // 80分钟 = 1.5小时
                {110, 2},    // 110分钟 = 2小时 (1小时 + 50分钟算0.5)
                {111, 2},    // 111分钟 = 2小时 (1小时 + 51分钟算1)
                {120, 2},    // 120分钟 = 2小时
                {130, 2},    // 130分钟 = 2小时 (2小时 + 10分钟算0)
                {141, 2.5f}, // 141分钟 = 2.5小时 (2小时 + 21分钟算0.5)
            };

            System.out.println("分钟数\t预期值\t实际值\t结果");
            for (float[] testCase : testCases) {
                long minutes = (long) testCase[0];
                float expected = testCase[1];
                float actual = (float) method.invoke(listener, minutes);
                String result = Math.abs(actual - expected) < 0.001f ? "✓" : "✗";
                System.out.println(minutes + "\t" + expected + "\t" + actual + "\t" + result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
