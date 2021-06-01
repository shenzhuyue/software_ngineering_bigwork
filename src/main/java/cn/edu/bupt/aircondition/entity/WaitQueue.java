package cn.edu.bupt.aircondition.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import static cn.edu.bupt.aircondition.entity.Constant.*;

/**
 *
 * @about 等待队列包含：高、中、低三个等级的队列
 * @author syl
 */
@Data
public class WaitQueue {

    private List<WaitObject> queueH = new ArrayList<>();

    private List<WaitObject> queueM = new ArrayList<>();

    private List<WaitObject> queueL = new ArrayList<>();

    /**
     * 插入等待对象
     * @param waitObject
     */
    public void insert(WaitObject waitObject){
        if (waitObject.getSpeed().equals(SPEED_H)){
            queueH.add(waitObject);
        }else if (waitObject.getSpeed().equals(SPEED_M)){
            queueM.add(waitObject);
        }else if (waitObject.getSpeed().equals(SPEED_L)){
            queueL.add(waitObject);
        }
    }

    public void insert(List<WaitObject> waitObjectList){
        for (WaitObject w:waitObjectList) {
            insert(w);
        }
    }

    /**
     *  删除等待队列中的等待对象
     * @param waitObject
     * @return
     */
    public void delete(WaitObject waitObject){
        if (waitObject.getSpeed().equals(SPEED_H)){
            queueH.remove(waitObject);
        }else if (waitObject.getSpeed().equals(SPEED_M)){
            queueM.remove(waitObject);
        }else if (waitObject.getSpeed().equals(SPEED_L)){
            queueL.remove(waitObject);
        }
    }
    /**
     * 删除等待队列中的制定空调的旧请求
     * @param acId
     * @return true 存在, false 不存在, 若存在旧请求,删除旧请求
     */
    public void delete(Integer acId){
        queueH.removeIf(w -> w.getAcId().equals(acId));
        queueM.removeIf(w -> w.getAcId().equals(acId));
        queueL.removeIf(w -> w.getAcId().equals(acId));
    }

    /**
     *
     * @param acId
     * @return true 等待队列中已存在对应空调的请求， false 等待队列中不存在对应请求
     */

    public Boolean check(Integer acId){
        for (WaitObject w:queueH) {
            if (w.getAcId().equals(acId)){
                return true;
            }
        }
        for (WaitObject w:queueM) {
            if (w.getAcId().equals(acId)){
                return true;
            }
        }
        for (WaitObject w:queueL) {
            if (w.getAcId().equals(acId)){
                return true;
            }
        }
        return false;
    }
    /**
     * 更新等待时长
     */
    public void update(){
        for (WaitObject w:queueH) {
            w.setTimeToWait(w.getTimeToWait() - 1);
        }
        for (WaitObject w:queueM) {
            w.setTimeToWait(w.getTimeToWait() - 1);
        }
        for (WaitObject w:queueL) {
            w.setTimeToWait(w.getTimeToWait() - 1);
        }

    }

    /**
     *
     * @return waitObject 为空表示等待队列中无请求， 否则返回等待对象
     */
    public WaitObject schedule(){
        if (! queueH.isEmpty()){
            return timeSchedule(queueH);
        }else if (! queueM.isEmpty()){
            return timeSchedule(queueM);
        }else if (! queueL.isEmpty()){
            return timeSchedule(queueL);
        }else {
            //等待队列为空
            return null;
        }
    }



    /**
     * 时间片
     * @param queue not null
     * @return waitObject
     */
    private WaitObject timeSchedule(List<WaitObject> queue){
        WaitObject waitObject = null;
        for (WaitObject w: queue){
            //选出等待时长 <=0
            if (w.getTimeToWait() <= 0){
                waitObject = w;
            }
        }
        //如果没有符合时间片调度的等待对象
        if (waitObject == null){
            waitObject = queue.get(0);
            for (WaitObject w:queue) {
                //选择等待时长最小的等待对象
                if (w.getTimeToWait() < waitObject.getTimeToWait()){
                    waitObject = w;
                }
            }
        }
        queue.remove(waitObject);
        return waitObject;
    }


}
