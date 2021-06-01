package cn.edu.bupt.aircondition.entity;

import lombok.Data;

import java.math.BigDecimal;


@Data
public class ReportItem {
    private Integer acId;//空调ID
    private Integer useTimes;//使用空调次数
    private Double commonTargetTemperature;//最常用目标温度
    private Integer commonSpeed;//最常用风速
    private Integer reachTimes;//到达目标温度次数
    private Integer scheduleTimes;//被调度次数
    private Integer detailRecordTimes;//详单记录数,记录空调服务日志
    private Double cost;//总费用
}
