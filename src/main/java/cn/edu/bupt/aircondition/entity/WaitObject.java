package cn.edu.bupt.aircondition.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WaitObject {

    private Integer acId;

    /**
     * @value: 0 低速, 1 中速, 2高速
     */
    private Integer speed;

    /**
     * @value: 0 制冷, 1制热
     */
//
//    private Integer mode;

//    private BigDecimal targetTempt;

    private Integer timeToWait;

    //by zhicheng lee
    public WaitObject(){}

    //by zhicheng lee
    public WaitObject(Integer acId, Integer speed, Integer timeToWait){
        this.acId=acId;
        this.speed=speed;
//        this.targetTempt= targetTempt;
        this.timeToWait=timeToWait;
    }
}
