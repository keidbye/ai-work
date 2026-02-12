package com.example.aiwork.utils;

import com.alibaba.excel.EasyExcel;

/**
 * @Author: ljf
 * @Description:
 * @Date: Create in 19:21 2024/4/8
 */
public class ExcelUtils {

    public static void main(String[] args) {
        //1.设置写入文件夹地址和excel文件名称
        String filename = "C:\\Users\\acer\\Desktop\\1上下班打卡_日报_20240301-20240331.xlsx";

        //2.实现读操作
        //read方法的第一个参数时：读取的文件路径，第二个参数是：实体类的class，第三个参数是：监听器
        EasyExcel.read(filename,WorkVo.class,new WorkDataListener()).sheet().headRowNumber(2).doRead();
    }

}
