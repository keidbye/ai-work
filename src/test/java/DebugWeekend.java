import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.context.AnalysisContext;
import com.example.aiwork.utils.WorkVo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DebugWeekend {
    public static void main(String[] args) {
        String filename = "/opt/excel/上下班打卡_日报_20260101-20260131(1).xlsx";

        final List<String> restDays = new ArrayList<>();
        // 不设置休息日，自动识别周末

        final StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("=== 雷杰飞周末加班明细 ===\n");

        EasyExcel.read(filename, WorkVo.class, new ReadListener<WorkVo>() {
            private float totalWeekendHours = 0f;

            @Override
            public void invoke(WorkVo data, AnalysisContext context) {
                if ("雷杰飞".equals(data.getName())) {
                    String dateTime = data.getDataTime();
                    if (dateTime != null && !dateTime.isEmpty()) {
                        String datePart = dateTime.split(" ")[0];
                        String weekDay = dateTime;

                        boolean isWeekend = weekDay.contains("星期六") || weekDay.contains("星期日");

                        if (isWeekend) {
                            String workHourNum = data.getWorkHourNum();
                            debugInfo.append("日期: ").append(datePart)
                                .append(" (").append(weekDay.split(" ")[1] != null ? weekDay.split(" ")[1] : weekDay)
                                .append(") 工作时长: ").append(workHourNum).append("\n");

                            if (workHourNum != null && !workHourNum.equals("--")) {
                                try {
                                    float hours = Float.parseFloat(workHourNum);
                                    totalWeekendHours += hours;
                                } catch (Exception e) {}
                            }
                        }
                    }
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                debugInfo.append("\n雷杰飞周末加班总计: ").append(totalWeekendHours).append(" 小时\n");
                System.err.println(debugInfo.toString());
            }
        }).sheet().headRowNumber(4).doRead();
    }
}
