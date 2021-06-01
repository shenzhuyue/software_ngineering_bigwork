package cn.edu.bupt.aircondition.entity;



import lombok.Data;

import java.util.Date;

@Data
public class ScheduleLog {

    private Integer acId;   //空调ID

    private Integer type;   //被调度类型: 1 调出, 2 调入

    private Integer serveId;    // 若为调出, 表示之前服务的服务对象， 若为调入， 表示将要服务的服务对象

    private Date date;  // 被调度时，系统时间

    public ScheduleLog(Integer acId, Integer type, Integer serveId, Date date){
        this.acId = acId;
        this.date = date;
        this.serveId = serveId;
        this.type = type;
    }
}

