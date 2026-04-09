package com.example.aiwork.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;

import java.util.HashSet;
import java.util.Set;

public class ListAllEmployees {
    public static void main(String[] args) {
        String filename = "/root/.openclaw/workspace/ai-work/上下班打卡_日报_20260201-20260228 (1).xlsx";

        Set<String> allEmployees = new HashSet<>();
        Set<String> employeesWithValidData = new HashSet<>();

        EasyExcel.read(filename, WorkVo.class, new ReadListener<WorkVo>() {
            @Override
            public void invoke(WorkVo data, AnalysisContext context) {
                // 统计所有出现的员工
                if (data.getName() != null && !data.getName().isEmpty()) {
                    allEmployees.add(data.getName());
                }

                // 统计有有效打卡数据的员工（有开始或结束时间）
                String startTime = data.getStartTime();
                String endTime = data.getEndTime();
                String vacation = data.getVacation();

                boolean hasValidTime = (startTime != null && !startTime.isEmpty() &&
                        isValidTimeString(startTime)) ||
                        (endTime != null && !endTime.isEmpty() &&
                                isValidTimeString(endTime));

                if (hasValidTime) {
                    employeesWithValidData.add(data.getName());
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                System.out.println("========== Excel中的所有员工 (共" + allEmployees.size() + "人) ==========");
                allEmployees.stream().sorted().forEach(name -> System.out.println(name));

                System.out.println("\n========== 有有效打卡数据的员工 (共" + employeesWithValidData.size() + "人) ==========");
                employeesWithValidData.stream().sorted().forEach(name -> System.out.println(name));

                System.out.println("\n========== 只有请假但无有效打卡数据的员工 ==========");
                allEmployees.stream()
                        .filter(name -> !employeesWithValidData.contains(name))
                        .sorted()
                        .forEach(name -> System.out.println(name + " - 只有请假记录，没有有效打卡数据"));
            }
        }).sheet().headRowNumber(4).doRead();
    }

    public static boolean isValidTimeString(String timeString) {
        try {
            if (timeString == null || timeString.isEmpty()) {
                return false;
            }
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            java.time.LocalTime time = java.time.LocalTime.parse(timeString, formatter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
