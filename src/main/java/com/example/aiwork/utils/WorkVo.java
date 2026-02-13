package com.example.aiwork.utils;

import com.alibaba.excel.annotation.ExcelProperty;

/**
 * @Author: ljf
 * @Description:
 * @Date: Create in 19:27 2024/4/8
 */

public class WorkVo {


    @ExcelProperty(value = {"时间","时间"})
    private String dataTime;

    @ExcelProperty(value = {"姓名","姓名"})
    private String name;

    @ExcelProperty(value = {"考勤概况","最早"})
    private String startTime;

    @ExcelProperty(value = {"考勤概况","最晚"})
    private String endTime;

    @ExcelProperty(value = {"考勤概况","实际工作时长(小时)"})
    private String workHourNum;

    @ExcelProperty(value = {"考勤概况","假勤申请"})
    private String vacation;




    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDataTime() {
        return dataTime;
    }

    public void setDataTime(String dataTime) {
        this.dataTime = dataTime;
    }

    public String getVacation() {
        return vacation;
    }

    public void setVacation(String vacation) {
        this.vacation = vacation;
    }

    public String getWorkHourNum() {
        return workHourNum;
    }

    public void setWorkHourNum(String workHourNum) {
        this.workHourNum = workHourNum;
    }

    @Override
    public String toString() {
        return "WorkVo{" +
                "name='" + name + '\'' +
                ", dataTime='" + dataTime + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", vacation='" + vacation + '\'' +
                '}';
    }
}
