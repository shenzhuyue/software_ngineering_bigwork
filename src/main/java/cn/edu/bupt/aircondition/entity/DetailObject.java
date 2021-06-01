package cn.edu.bupt.aircondition.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class DetailObject {
    /**
    * 详单类
     * create by zhicheng lee
    * */

    private Integer acId;
    private Integer speed;  //value: 0 低速, 1 中速, 2高速
    private Integer mode;   //value: 0 制冷, 1制热
    private Date startTime;
    private Date endTime;
    private BigDecimal startTempt;
    private BigDecimal endTempt;
    private BigDecimal consumption; //高风：1度/1分钟 中风：1度/2分钟 低风：1度/3分钟
    private BigDecimal fee;

    public DetailObject() {}
}
