package cn.edu.bupt.aircondition.entity;


import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ServeObject {

    private Integer acId;

    //指服务对象Id
    private Integer serveId;

    /**
     * @value: 0 空闲, 1 服务中
     */
    private Integer state;

    /**
     * @value: 0 制冷, 1制热
     */
    private Integer mode;

    /**
     * @value: 0 低速, 1 中速, 2高速
     */
    private Integer speed;


    private BigDecimal startTemp;

    private BigDecimal targetTemp;//目标温度

    private BigDecimal currentTemp;//保存时当作结束温度

    private BigDecimal feeRate;

    private BigDecimal fee;

    private Date startTime;

    private Date endTime;

    //服务时长
    private Integer serveTime;


}
