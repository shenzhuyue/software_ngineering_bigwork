package cn.edu.bupt.aircondition.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static cn.edu.bupt.aircondition.entity.Constant.*;

/**
 * @about: 服务队列
 * @author ssyl
 */
@Data
public class ServeQueue {

    private List<ServeObject> serveObjectList;

    /**
     * 初始化
     * @param numOfServeObject
     */
    public void init(Integer numOfServeObject){
        serveObjectList = new ArrayList<>();
        for (int i = 0; i < numOfServeObject ; i++) {
            ServeObject serveObject = new ServeObject();
            serveObject.setServeId(i);
            serveObjectList.add(i, serveObject);
        }
    }

    /**
     * 设置默认模式
     * @param mode 模式
     * @param speed 风速
     * @param feeRate 费率
     * @param startTempt 开始
     * @param currentTempt
     * @param fee
     */
    public void setDefaultPara(Integer mode, Integer speed, BigDecimal feeRate, BigDecimal startTempt, BigDecimal currentTempt, BigDecimal targetTempt, BigDecimal fee){
        for (ServeObject s:serveObjectList) {
            s.setTargetTemp(targetTempt);
            s.setMode(mode);
            s.setSpeed(speed);
            s.setFeeRate(feeRate);
            s.setStartTemp(startTempt);
            s.setCurrentTemp(currentTempt);
            s.setFee(fee);
        }
    }

    /**
     * 用于初始化时，设置所有服务对象空闲
     */
    public void setReady(){
        for (ServeObject s:serveObjectList) {
            s.setState(Constant.SERVE_IDLE);
        }
    }

    /**
     *
     *
     * @return -1 表示没有空闲的服务对象, [0-numOfServeObject-1]表示有空闲的服务对象，返回值表示服务对象ID，也是服务对象在服务队列中的位置
     */
    public Integer hasIdle(){
        for (ServeObject s:serveObjectList) {
            if (s.getState().equals(SERVE_IDLE)){
                return s.getServeId();
            }
        }
        return -1;
    }
    /**
     *
     * @param speed 请求服务的风速
     * @return 1 大于某个/某些服务对象优先级 0 等于某个/某些服务对象优先级 -1小于所有服务对象优先级
     */
    public Integer comparePriority(Integer speed){
        Integer cSpeed = SPEED_H;
        for (ServeObject s:serveObjectList) {
            if (s.getSpeed() < cSpeed){
                cSpeed = s.getSpeed();
            }
        }
        if (cSpeed > speed){
            return -1;
        }else if (cSpeed.equals(speed)){
            return 0;
        }else{
            return 1;
        }
    }
    /**
     * 获取服务对象
     */
    public ServeObject getServeObject(Integer serveId){
        return serveObjectList.get(serveId);
    }
    /**
     * 检查是否空调被服务
     * @return null 没有被服务，  正在提供服务的服务对象
     */
    public ServeObject check(Integer acId){
        for (ServeObject s:serveObjectList) {
            if (s.getState().equals(SERVE_RUNNING) &&s.getAcId().equals(acId)){
                return s;
            }
        }
        return null;
    }
}
