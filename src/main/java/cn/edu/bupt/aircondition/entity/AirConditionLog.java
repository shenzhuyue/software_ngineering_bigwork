package cn.edu.bupt.aircondition.entity;

import lombok.Data;

import java.util.Date;

/**
 * 空调开机关机请求 存储数据库的实体
 */
@Data
public class AirConditionLog {

    private Integer acId;   //空调ID

    private Integer state;  //采用Constant中的常量STATE_ON, STATE_OFF

    private Date date;  //状态变化时的系统时间

    public AirConditionLog(Integer acId, Integer state, Date date){
        this.acId = acId;
        this.date = date;
        this.state = state;
    }
}
