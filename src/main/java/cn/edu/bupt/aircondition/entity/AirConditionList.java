package cn.edu.bupt.aircondition.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static cn.edu.bupt.aircondition.entity.Constant.*;

@Data
public class AirConditionList {
    private List<AirCondition> airConditionList;

    public AirConditionList(){
        airConditionList = new ArrayList<>(NUM_OF_AIR_CONDITION);
        for (int i = 0; i < NUM_OF_AIR_CONDITION; i++) {
            AirCondition airCondition = new AirCondition();
            airCondition.setAcId(i);
            airCondition.setState(STATE_OFF);
            airConditionList.add(i, airCondition);
        }
    }

    /**
     * 设置默认开机参数
     */
    public void setDefaultPara(BigDecimal temptDefault, Integer modeDefault, BigDecimal targetTempt, Integer speedDefault, Integer state, BigDecimal fee){
        //设置空调默认风速， 默认初始温度， 默认当前温度， 默认目标温度， 默认模式
        for (AirCondition a:airConditionList) {
            a.setCurrentTemperature(temptDefault);
            a.setOriginTemperature(temptDefault);
            a.setMode(modeDefault);
            a.setTargetTemperature(targetTempt);
            a.setSpeed(speedDefault);
            a.setState(state);
            a.setFee(fee);
        }
    }
    /**
     * 关机时的处理
     */
    public void setOff(){
        for (AirCondition a:airConditionList) {
            a.setState(STATE_OFF);
        }
    }
    /**
     * 获得房间状态
     */
    public AirCondition getAirCondition(Integer acId){
        return airConditionList.get(acId);
    }


    /**
     * 设置空调的状态为On
     */
    public void setAirConditionOn(Integer acId){
        AirCondition airCondition = airConditionList.get(acId);
        airCondition.setState(STATE_ON);
    }






}
