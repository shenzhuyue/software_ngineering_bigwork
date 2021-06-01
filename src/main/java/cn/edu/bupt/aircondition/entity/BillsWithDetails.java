package cn.edu.bupt.aircondition.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class BillsWithDetails {
    /**
     * 账单及详单类
     * create by zhicheng lee
     * */
    private Integer customerId;
    private String name;
    private Integer acId;//room id OR air condition id
    private Date startTime;
    private Date endTime;
    private BigDecimal allConsumption;
    private BigDecimal allFee;

    private List<DetailObject> detailObjectList;

    public BillsWithDetails() {
        this.detailObjectList=new ArrayList<>();
    }
}
