package cn.edu.bupt.aircondition.entity;

import lombok.Data;

@Data
public class Env {
    private AirConditionList airConditionList;
    private ServeQueue serveQueue;
    private WaitQueue waitQueue;
}
