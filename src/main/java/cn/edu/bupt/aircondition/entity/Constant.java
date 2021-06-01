package cn.edu.bupt.aircondition.entity;


import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @about 系统相关的常量,
 * @author ssyl
 */
public class Constant {
    public final static Integer SPEED_H = 2;//高风速
    public final static Integer SPEED_M = 1;//中风速
    public final static Integer SPEED_L = 0;//低风速

    public final static Integer STATE_ON = 1;//空调开启
    public final static Integer STATE_OFF = 2;//空调关闭
    public final static Integer STATE_RUNNING = 3;//空调运行

    public final static Integer MODE_H = 1;//制热
    public final static Integer MODE_R = 0;//制冷

    public final static Integer SERVE_IDLE = 1;//服务对象空闲
    public final static Integer SERVE_RUNNING = 2;// 服务对象服务中

    public final static Integer NUM_OF_AIR_CONDITION= 5;

    public static Integer SPEED_DEFAULT;//空调默认风速

    public static BigDecimal TEMPT_DEFAULT = new BigDecimal("25.0");//空调默认温度

    public static Integer MODE_DEFAULT;//空调默认模式

    public static BigDecimal TEMPT_HIGH_LIMIT;//空调默认最高温度

    public static BigDecimal TEMPT_LOW_LIMIT;//空调默认最低温度

    public static Map<Integer,BigDecimal> feeMap = new HashMap<>(); //费用
    public static Map<Integer,BigDecimal> temptMap = new HashMap<>();//温度


    //调度
    public final static Integer SCHEDULE_IN = 1;//调入服务队列
    public final static Integer SCHEDULE_OUT = 2;//调出服务队列

    //默认分配等待时长2分钟
    public final static Integer TIME_TO_WAIT = 2;

    //改为指定系统开始时间
    public static Date systemClock = new Date(System.currentTimeMillis());

    //报表类型
    public final static Integer REPORT_DAY = 1;//日报表
    public final static Integer REPORT_WEEK = 2;//周报表
    public final static Integer REPORT_MONTH = 3;//月报表
    public final static Integer REPORT_YEAR = 4;//年报表

    //是否到达目标温度
    public final static Integer REACH_TARGET = 1;
    public final static Integer NO_REACH_TARGET = 0;


    /**
     *
     * @param tempt 请求的温度
     * @return 返回对应的模式
     */
    public static Integer getMode(BigDecimal tempt){
        return MODE_DEFAULT;
    }

}
