package cn.edu.bupt.aircondition.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Data
public class Report {

    private Date start_time;
    private Date end_time;
    private List<ReportItem> reportItemList;

    public Report(){
        reportItemList = new ArrayList<ReportItem>();
    }

}
