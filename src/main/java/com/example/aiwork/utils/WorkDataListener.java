package com.example.aiwork.utils;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Author: ljf
 * @Description:
 * @Date: Create in 19:27 2024/4/8
 */
public class WorkDataListener implements ReadListener<WorkVo> {

	/**
	 * 填入当月休息日日期，统计加班小时数
	 * @param   日期 yyyy/MM/dd  如2024/12/21
	 *
	 */
	private List<String> REST_DAY_LIST = new ArrayList<>();

	/**
	 * 特殊考勤规则员工名单
	 * 统计规则：工作日19~21点加班小时数，21~次日凌晨5点加班小时数
	 * @param   姓名
	 *
	 */
	private List<String> FILTER_NAME_LIST = new ArrayList<>();

	public WorkDataListener() {
		// 默认空列表
	}

	public WorkDataListener(List<String> restDayList, List<String> filterNameList) {
		this.REST_DAY_LIST = restDayList != null ? restDayList : new ArrayList<>();
		this.FILTER_NAME_LIST = filterNameList != null ? filterNameList : new ArrayList<>();
	}

	/**
	 * 考勤逻辑：
	 * 
	 * 1、头天晚加到22点，次若上午10：00前打卡算正常；否则算当天迟到；  -- 废弃
	 * 2、例外：当天9点前未打卡算缺卡；加班到24点后，加班时间算到当天晚上24点；并且第二天13：30前打卡算正常； -- 废弃
	 *
	 * 25-04
	 * 1、头天晚上加班到23点，次日上午9:30前打卡算正常；
	 * 2、头天晚上加班到24点，次日上午10点前打卡算正常；
	 * 3、请假、迟到、未打卡一次扣除一次加班补贴
	 * 4、休息日加班补贴，工作时长一天>4小时 <8小时 算1次； >8小时算2次； 一天最多算2次
	 *
	 * hourNum：			小时数（普通员工19:00后，特殊规则员工不使用此字段）
	 * hour19To21Num：	19:00-21:00 加班小时数（特殊规则员工）
	 * hour21To05Num：	21:00-次日05:00 加班小时数（特殊规则员工）
	 * name:				姓名
	 * leaveNum：			休假数
	 * noCheckInNum：		未打卡数
	 * lateNum：			迟到数
	 * dayNum：				正常打卡数
	 * restDayWordNum：		休息日工作时长
	 **/

	public static void main(String[] args) {

		// 1.设置写入文件夹地址和excel文件名称
		String filename = "/opt/excel/上下班打卡_日报_20260101-20260131(1).xlsx";

		// 2.表中存在多人的记录 --> [{hourNum=33, leaveNum=0, name=雷杰飞, noCheckInNum=0, lateNum=0, dayNum=15}]
		List<Map<String, Object>> counts = getCounts(filename);
		// System.out.println(counts);

		for (Map<String, Object> map : counts) {
			System.out.println(map.get("name") + "-天数：" + map.get("dayNum") + "-小时数：" + map.get("hourNum") + "-请假数："
					+ map.get("leaveNum") + "-未打卡数：" + map.get("noCheckInNum") + "-迟到天数：" + map.get("lateNum")
					+ "-周末小时：" + map.get("restDayWordNum") + "-周末补贴次数：" + map.get("subsidyNum")
					+ "-19:00-21:00加班小时：" + map.get("hour19To21Num") + "-21:00-05:00加班小时：" + map.get("hour21To05Num"));
		}

		// 3.表中单人的记录 --> {hourNum=36, name=雷杰飞, noCheckInNum=1, dayNum=18 }
		// Map<String, Object> count = getCount(filename);
		// System.out.println(count);

	}

	/**
	 * 集合初始容量
	 */
	private static final int BATCH_COUNT = 500;

	/**
	 * 定义一个标准时间格式化器，这里使用24小时制
	 */
	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

	/**
	 * 未打卡次数
	 */
	private static Map<String,Integer> noCheckInMap = new HashMap<>();

	/**
	 * 请假
	 */
	private static Map<String,Integer> leaveMap = new HashMap<>();

	/**
	 * 迟到次数
	 */
	private static Map<String,Integer> lateNumMap = new HashMap<>();

	/**
	 * 休息日工作时长数
	 */
	private static Map<String,Float> restDayNumMap = new HashMap<>();

	/**
	 * 休息日补贴次数
	 */
	private static Map<String,Integer> subsidyNumMap = new HashMap<>();

	/**
	 * 19:00-21:00 加班小时数（特殊规则员工）
	 */
	private static Map<String,Integer> hour19To21NumMap = new HashMap<>();

	/**
	 * 21:00-次日05:00 加班小时数（特殊规则员工）
	 */
	private static Map<String,Integer> hour21To05NumMap = new HashMap<>();

	/**
	 * 缓存的数据
	 */
	private List<WorkVo> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);


	/**
	 * 超过9点计为迟到数据
	 *
	 */

	private Map<String,List<WorkVo>> lateWorkVoMap = new HashMap<>();

	/**
	 * 超过10点的数据
	 */
	private Map<String, WorkVo> lateDataMap = new HashMap<>();
	private List<Map<String, Object>> countMap = new ArrayList<>();

	/**
	 * 这个每一条数据解析都会来调用
	 *
	 * @param data
	 *            one row value. Is is same as
	 *            {@link AnalysisContext#readRowHolder()}
	 * @param context
	 */
	@Override
	public void invoke(WorkVo data, AnalysisContext context) {

		// 生成早上未打卡记录
		if (noCheckInMap.get(data.getName()) == null){
			noCheckInMap.put(data.getName(),0);
			lateNumMap.put(data.getName(),0);

		}
		String dataTime = data.getDataTime();

		// 判断当前日志是否为休息日,并统计加班时长
		countRestDayWorkNum(data);


		if (StringUtils.isEmpty(dataTime) ){
			return;
		}



		// 统计事假/调休假/年假等等 —> 算当天未打卡
		String vacation = data.getVacation();
		List<String> vacationKey = Arrays.asList("年假", "事假", "病假","调休假","婚假","产假","陪产假");

		if (!StringUtils.isEmpty(vacation) && vacationKey.stream().anyMatch(vacation::contains)){
			Integer leaveNum = leaveMap.get(data.getName()) ;
			leaveMap.put(data.getName(),leaveNum == null ? 1:leaveNum+1);
			return;
		}





		if (!isValidTimeString(data.getStartTime()) && !isValidTimeString(data.getEndTime())) {
			return;
		}

		// 星期六星期日不统计
		String[] time = dataTime.split(" ");
		if ( REST_DAY_LIST.contains(time[0]) ){
			return;
		}else if ((dataTime.contains("星期六") || dataTime.contains("星期日")) && isTimeBefore(data.getStartTime(), "10:01")){
			data.setStartTime("09:00");
		}else if ((dataTime.contains("星期六") || dataTime.contains("星期日")) && !isTimeBefore(data.getStartTime(), "10:00")){
			lateNumMap.put(data.getName(),lateNumMap.get(data.getName())+1);
			data.setStartTime("09:40");
		}


		cachedDataList.add(data);



		if (!isTimeBefore(data.getStartTime(), "09:30")) {
			// 统计
			//noCheckInMap.put(data.getName(),noCheckInMap.get(data.getName())+1);
			lateDataMap.put(data.getName(), data);
			//return;
		}else if (!isTimeBefore(data.getStartTime(), "09:01")) {
			// 统计
			lateNumMap.put(data.getName(),lateNumMap.get(data.getName())+1);

			List<WorkVo> workVos = lateWorkVoMap.get(data.getName());
			if (workVos == null){
				workVos = new ArrayList<>();
			}
			workVos.add(data);
			lateWorkVoMap.put(data.getName(), workVos);
		}


		if (data.getEndTime().contains("次日")) {
			// 判断是否有次日的数据
			data.setEndTime("23:59");

			// 处理日期
			String dateTime = data.getDataTime().split(" ")[0];
			// 获取次日的日期
			dateTime = subtractOneDayFromDate(dateTime, 1);
			WorkVo workVo = lateDataMap.get(data.getName());
			// 如果存在，证明第二天早上休息，打卡时间置为 09：00
			if (workVo != null) {


				String newDateTime = workVo.getDataTime().split(" ")[0];

				// 当天13：30 之前算正常 -- 修改为 凌晨24点后,需早上10点打开算正常
				String startTime = workVo.getStartTime();
				if (!isTimeBefore(startTime,"10:00")){
					Integer integer = lateNumMap.get(workVo.getName());
					lateNumMap.put(workVo.getName(),integer-1);
					//return;
				}else if (newDateTime.equals(dateTime)) {
					workVo.setStartTime("09:00");
					// cachedDataList.add(workVo);
					Integer integer = noCheckInMap.get(workVo.getName());
					noCheckInMap.put(workVo.getName(),integer-1);
				}

			}
		}else if (!isTimeBefore(data.getEndTime(), "23:00")) {
			// todo 判断昨日是否为晚上10点之后,是则迟到次数-1, 后面修改为 23:00

			// 处理日
			String dateTime = data.getDataTime().split(" ")[0];
			// 获取次日的日期
			dateTime = subtractOneDayFromDate(dateTime, 1);
			List<WorkVo> workVos = lateWorkVoMap.get(data.getName());

			if (workVos != null){
				for (WorkVo workVo : workVos) {
					String newDateTime = workVo.getDataTime().split(" ")[0];

					if (newDateTime.equals(dateTime)) {
						Integer integer = lateNumMap.get(workVo.getName());
						lateNumMap.put(workVo.getName(),integer-1);
					}
				}
			}


		}


	}

	/**
	 * 数据解析完成后执行业务逻辑
	 *
	 * @param context
	 */
	@Override
	public void doAfterAllAnalysed(AnalysisContext context) {

		// 保存次日打卡的
		// Map<>

		Map<String, List<WorkVo>> listMap = cachedDataList.stream().collect(Collectors.groupingBy(WorkVo::getName));

		for (String key : listMap.keySet()) {

			AtomicInteger dayNum = new AtomicInteger();
			AtomicInteger hourNum = new AtomicInteger();
			AtomicInteger noCheckInNum = new AtomicInteger();
			// 初始化特殊规则员工的加班统计
			if (FILTER_NAME_LIST.contains(key)) {
				hour19To21NumMap.put(key, 0);
				hour21To05NumMap.put(key, 0);
			}

			List<WorkVo> workVos = listMap.get(key);
			boolean isSpecialEmployee = FILTER_NAME_LIST.contains(key);

			workVos.stream().forEach(workVo -> {
				if (isSpecialEmployee) {
					// 特殊规则员工：计算19:00-21:00和21:00-05:00两个时段的加班小时数
					calculateSpecialOvertimeHours(workVo, key);
				}

				// 统一统计：工作日加班小时数和天数
				Long num = calculateHoursDifference(workVo.getEndTime(), "19:00");
				if (num >= 2) {
					dayNum.getAndIncrement();
				}
				hourNum.addAndGet(num.intValue());

				// 未打卡天数 "最晚"时间早于12点
				if (isTimeBefore(workVo.getEndTime(), "10:00")) {
					noCheckInNum.getAndIncrement();
				}

			});

			HashMap<String, Object> map = new HashMap<>();
			map.put("name", key);
			map.put("dayNum", dayNum);
			// 特殊规则员工不统计小时数
			map.put("hourNum", isSpecialEmployee ? 0 : hourNum);

			int noCheckIn = noCheckInNum.intValue() + noCheckInMap.get(key);
			map.put("noCheckInNum", noCheckIn>0?noCheckIn:0);
			map.put("lateNum", lateNumMap.get(key));
			map.put("leaveNum", leaveMap.get(key) == null?0:leaveMap.get(key));
			map.put("restDayWordNum", restDayNumMap.get(key) == null?0:roundToOneDecimal(restDayNumMap.get(key)));

			// 特殊规则员工不统计周末补贴次数
			int subsidyNum = isSpecialEmployee ? 0 : (subsidyNumMap.get(key) == null ? 0 : subsidyNumMap.get(key));
			map.put("subsidyNum", subsidyNum);

			// 特殊规则员工：添加两个时段的加班小时数
			map.put("hour19To21Num", hour19To21NumMap.getOrDefault(key, 0));
			map.put("hour21To05Num", hour21To05NumMap.getOrDefault(key, 0));

			countMap.add(map);
		}

	}

	/**
	 * 校验字符串是否为时间
	 *
	 * @param timeString
	 * @return
	 */
	public static boolean isValidTimeString(String timeString) {
		try {

			if (StringUtils.isEmpty(timeString)) {
				return false;
			}

			// 尝试解析字符串为LocalTime对象
			LocalTime time = LocalTime.parse(timeString, formatter);

			// 如果解析成功且得到的LocalTime对象有效（即时间值在合理范围内），则返回true
			return !time.isBefore(LocalTime.MIN) && !time.isAfter(LocalTime.MAX);
		} catch (DateTimeParseException e) {
			// 解析过程中发生异常，说明字符串不符合预期的时间格式，返回false
			return false;
		}
	}

	/**
	 * 计算小时数
	 * 
	 * @param timeString1
	 * @param timeString2
	 * @return
	 */
	public static long calculateHoursDifference(String timeString1, String timeString2) {
		try {
			int hourNum = 0;
			if (ObjectUtil.equals(timeString1,"23:59")){
				hourNum ++;
				timeString1 = "23:00";
			}

			LocalTime time1 = LocalTime.parse(timeString1, formatter);
			LocalTime time2 = LocalTime.parse(timeString2, formatter);

			// 下班打卡时间要比 19:00 晚

			if (time1.isAfter(time2)) {
				Duration difference = Duration.between(time1, time2);

				return Math.abs(difference.toHours()) > 4 ? 4 : Math.abs(difference.toHours()) + hourNum;
			}
		}catch (Exception e){
			e.printStackTrace();
		}


		return 0;

	}

	/**
	 * 筛选某个时间范围内的数据 条件：开始时间<09：59
	 * 
	 * @param startTimeStr
	 * @param referenceTimeStr
	 * @return
	 */
	public static boolean isTimeBefore(String startTimeStr, String referenceTimeStr) {

		LocalTime parsedTime = LocalTime.parse(startTimeStr, formatter);

		return parsedTime.isBefore(LocalTime.parse(referenceTimeStr, formatter));
	}

	/**
	 * 时间 + 1
	 * 
	 * @param dateString
	 * @param dayNum
	 * @return
	 */
	public static String subtractOneDayFromDate(String dateString, int dayNum) {
		// 将时间字符串解析为 LocalDate 对象
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		LocalDate date = LocalDate.parse(dateString, formatter);
		LocalDate previousDay = date;
		// 将日期加 N 天
		if (dayNum > 0) {
			previousDay = date.plusDays(dayNum);
		} else if (dayNum < 0) {
			// 将日期减 N 天
			previousDay = date.minusDays(-dayNum);
		}

		// 将日期格式化为字符串并返回
		return previousDay.format(formatter);
	}

	/**
	 * 计算休息日工作时长
	 * @param work
	 */
	private void countRestDayWorkNum(WorkVo work){

		if (StringUtils.isEmpty(work.getDataTime())){
			return;
		}

		String dataTime = work.getDataTime().split(" ")[0];
		String workHourNum = extractNumber(work.getWorkHourNum());

		// 符合条件则累加加班时间
		if (REST_DAY_LIST.contains(dataTime) && workHourNum.matches("[-+]?[0-9]*\\.?[0-9]+") ){
			Float num = restDayNumMap.get(work.getName()) == null? 0f :restDayNumMap.get(work.getName());
			restDayNumMap.put(work.getName(),num + Float.valueOf(workHourNum));

			Integer subsidyNum = subsidyNumMap.get(work.getName());
			int newNum = 0;
			if (Float.valueOf(workHourNum)>=8f){
				newNum = 2;
			}else if (Float.valueOf(workHourNum)>=4f){
				newNum = 1;
			}
			subsidyNumMap.put(work.getName(),subsidyNum == null ? 0+newNum:subsidyNum+newNum);


		}

	}

	public static List<Map<String, Object>> getCounts(String fileName) {
		return getCounts(fileName, null, null);
	}

	public static List<Map<String, Object>> getCounts(String fileName, List<String> restDayList, List<String> filterNameList) {
		// read方法的第一个参数时：读取的文件路径，第二个参数是：实体类的class，第三个参数是：监听器
		WorkDataListener listener = new WorkDataListener(restDayList, filterNameList);
		EasyExcel.read(fileName, WorkVo.class, listener).sheet().headRowNumber(4).doRead();
		return listener.getCountMap();
	}

	public List<Map<String, Object>> getCountMap() {
		return countMap;
	}

	public static Map<String, Object> getCount(String fileName) {
		return getCounts(fileName).get(0);
	}


	/**
	 * 计算特殊规则员工的加班小时数
	 * 19:00-21:00 和 21:00-次日05:00 两个时段
	 *
	 * @param workVo 考勤记录
	 * @param employeeName 员工姓名
	 */
	private void calculateSpecialOvertimeHours(WorkVo workVo, String employeeName) {
		String endTime = workVo.getEndTime();
		boolean isNextDay = endTime.contains("次日");

		try {
			// 解析结束时间
			String endTimeStr = isNextDay ? endTime.replace("次日", "").trim() : endTime;
			if (endTimeStr.isEmpty()) {
				return;
			}

			LocalTime endLocalTime = LocalTime.parse(endTimeStr, formatter);

			// 19:00 的基准时间
			LocalTime time19 = LocalTime.of(19, 0);
			LocalTime time21 = LocalTime.of(21, 0);
			LocalTime time05 = LocalTime.of(5, 0);

			if (isNextDay) {
				// 加班到次日
				// 19:00-21:00: 2小时
				hour19To21NumMap.put(employeeName, hour19To21NumMap.getOrDefault(employeeName, 0) + 2);

				// 21:00-24:00: 3小时
				int hours21To24 = 3;
				hour21To05NumMap.put(employeeName, hour21To05NumMap.getOrDefault(employeeName, 0) + hours21To24);

				// 次日 00:00-05:00
				if (endLocalTime.isBefore(time05) || endLocalTime.equals(time05)) {
					int hoursNextDay = (int) Duration.between(LocalTime.MIN, endLocalTime).toHours();
					hour21To05NumMap.put(employeeName, hour21To05NumMap.getOrDefault(employeeName, 0) + hoursNextDay);
				} else if (endLocalTime.isAfter(time05)) {
					// 如果超过5点，只算到5点
					hour21To05NumMap.put(employeeName, hour21To05NumMap.getOrDefault(employeeName, 0) + 5);
				}
			} else {
				// 当天加班
				if (endLocalTime.isBefore(time21) || endLocalTime.equals(time21)) {
					// 19:00-21:00 之间加班
					if (endLocalTime.isAfter(time19)) {
						int hours = (int) Duration.between(time19, endLocalTime).toHours();
						hour19To21NumMap.put(employeeName, hour19To21NumMap.getOrDefault(employeeName, 0) + hours);
					}
				} else {
					// 19:00-21:00: 2小时
					hour19To21NumMap.put(employeeName, hour19To21NumMap.getOrDefault(employeeName, 0) + 2);

					// 21:00-结束时间
					int hours21ToEnd = (int) Duration.between(time21, endLocalTime).toHours();
					hour21To05NumMap.put(employeeName, hour21To05NumMap.getOrDefault(employeeName, 0) + hours21ToEnd);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 提取字符串中的数字（包括小数）
	 *
	 * @param input 输入的字符串
	 * @return 提取到的数字（如果没有数字，则返回 null）
	 */
	public static String extractNumber(String input) {
		if (input == null) {
			return "";
		}

		// 定义正则表达式，用于匹配整数或小数
		String regex = "-?\\d+\\.?\\d*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(input);

		// 如果找到匹配的数字，返回第一个匹配的值
		if (matcher.find()) {
			return matcher.group();
		}

		// 如果没有找到数字，返回 null
		return "";
	}

	/**
	 * 保留一位小数
	 * @param value
	 * @return
	 */
	public static float roundToOneDecimal(float value) {
		BigDecimal bd = new BigDecimal(value);
		// 保留一位小数，四舍五入
		bd = bd.setScale(1, RoundingMode.HALF_UP);
		// 转换回 float 类型
		return bd.floatValue();
	}

}