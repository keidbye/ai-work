package com.example.aiwork.utils;

import com.alibaba.excel.EasyExcel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestAttendance {
    public static void main(String[] args) {
        // 1.设置写入文件夹地址和excel文件名称
        String filename = "/root/.openclaw/workspace/ai-work/上下班打卡_日报_20260201-20260228 (1).xlsx";

        // 设置休息日列表
        List<String> restDayList = Arrays.asList(new String[] {
            "2026/02/01", "2026/02/08", "2026/02/15", "2026/02/16", "2026/02/17",
            "2026/02/18", "2026/02/19", "2026/02/20", "2026/02/21", "2026/02/22", "2026/02/23"
        });

        // 设置特殊规则员工名单
        List<String> filterNameList = Arrays.asList(new String[] {
        });

        // 2.统计考勤数据
        List<Map<String, Object>> counts = WorkDataListener.getCounts(filename, restDayList, filterNameList);

        // 打印所有人的考勤统计
        System.out.println("========== 所有员工考勤统计 ==========");
        for (Map<String, Object> map : counts) {
            String name = (String) map.get("name");
            System.out.println(name + "-正常天数：" + map.get("dayNum") + "-加班小时数：" + map.get("hourNum")
                    + "-请假数：" + map.get("leaveNum") + "-未打卡数：" + map.get("noCheckInNum")
                    + "-迟到次数：" + map.get("lateNum") + "-周末小时：" + map.get("restDayWordNum")
                    + "-周末补贴次数：" + map.get("subsidyNum"));
        }

        // 打印李世鹏的详细统计
        System.out.println("\n========== 李世鹏详细统计 ==========");
        for (Map<String, Object> map : counts) {
            if ("李世鹏".equals(map.get("name"))) {
                System.out.println("李世鹏考勤统计：");
                System.out.println("- 正常打卡天数：" + map.get("dayNum"));
                System.out.println("- 加班小时数：" + map.get("hourNum"));
                System.out.println("- 请假数：" + map.get("leaveNum"));
                System.out.println("- 未打卡数：" + map.get("noCheckInNum"));
                System.out.println("- 迟到次数：" + map.get("lateNum"));
                System.out.println("- 周末加班小时：" + map.get("restDayWordNum"));
                System.out.println("- 周末补贴次数：" + map.get("subsidyNum"));
                break;
            }
        }
    }
}
