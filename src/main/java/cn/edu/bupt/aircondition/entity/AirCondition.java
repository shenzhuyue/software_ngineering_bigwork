package cn.edu.bupt.aircondition.entity;


import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AirCondition {

    @TableId("acId")
    private Integer acId;
    /**
     * @value: 1 开启, 0 关闭, 2 运行中
     */
    private Integer state;
    /**
     * 自动设置
     * @value: 0 制冷, 1制热
     */
    private Integer mode;
    /**
     * @value: 0 低速, 1 中速, 2高速
     */
    private Integer speed;

    private BigDecimal currentTemperature;

    private BigDecimal targetTemperature;

    private BigDecimal originTemperature;

    //总费用: 空调从开机到关机总费用
    private BigDecimal fee;


}
