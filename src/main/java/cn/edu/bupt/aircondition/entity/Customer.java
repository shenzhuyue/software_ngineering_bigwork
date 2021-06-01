package cn.edu.bupt.aircondition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

import static cn.edu.bupt.aircondition.entity.Constant.systemClock;

@Data
public class Customer {
    @TableId(type = IdType.AUTO)
    private int customerId;
    private String name;
    private int acId;//air condition id
    private Date inTime;
    private Date outTime;

    public  Customer(){
        this.name=null;
        this.acId=-1;
        this.inTime=systemClock;
        this.outTime=systemClock;
    }
}
