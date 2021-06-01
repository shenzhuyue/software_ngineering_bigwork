package cn.edu.bupt.aircondition.controller;

import cn.edu.bupt.aircondition.common.Result;
import cn.edu.bupt.aircondition.entity.*;
import cn.edu.bupt.aircondition.mapper.AirConditionLogMapper;
import cn.edu.bupt.aircondition.mapper.ScheduleLogMapper;
import cn.edu.bupt.aircondition.mapper.ServeMapper;
import cn.edu.bupt.aircondition.entity.Env;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sun.tools.jconsole.JConsoleContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;


import java.math.BigDecimal;
import java.util.*;
import static cn.edu.bupt.aircondition.entity.Constant.*;

/**
 * @about  a restful controller , return json data
 *
 * @author syl(调度+系统管理员), lzc(前台 + 客户), zdx(酒店经理)
 */

/**
 * 基本步骤
 * 0、空调管理员中央空调参数配置
 * 1、前台用户入住登记
 * 2、客户空调测试
 * 3、客户退房，出详单
 * 4、酒店经理可在任何时间查询报表
 */
//todo: 设置的系统时间与本时区时间不符
//todo: 默认调度时的请求各个参数都为合法数据， 在此之前凡是需要进入调度的请求必须检查数据的合法性
@RestController
@RequestMapping
@Api(value = "中央空调系统接口测试")

public class SystemController {


    private ServeQueue serveQueue;

    private WaitQueue waitQueue;

    private AirConditionList airConditionList;

    private List<Customer> customerList;    //by zhicheng lee

    @Autowired
    private ServeMapper serveMapper;

    @Autowired
    private ScheduleLogMapper scheduleLogMapper;

    @Autowired
    private AirConditionLogMapper airConditionLogMapper;

    private Integer state = STATE_OFF;//标识中央空调的当前状态: STATE_ON, STATE_RUNNING, STATE_OFF

    private List<Integer> reachTarget;//标识空调是否达到目标温度


    /* *********************************客户相关**************************************/

    /**
     * @about: 接收空调开机请求，处理开机请求  by zhicheng lee
     */
    @ApiOperation(value = "开机请求")
    @GetMapping("/ac/start")
    public Result<?> start(@RequestParam Integer acId, @RequestParam Double cTempt) {

        /*不存在开机请求、关机请求，开机时要设置空调属性，然后发送默认的送风请求*/

        if (!state.equals(STATE_RUNNING)){
            return Result.error(500, "系统未运行!!");
        }

        /*不检查客户是否存在，因为空调管理员可以控制空调*/

        /*设置空调初始化参数*/
        /*fee、scheduleTime、useTime属性在客户入住时设置*/
        AirCondition airCondition=airConditionList.getAirCondition(acId);

        //判断空调是否已经开机
        if (!airCondition.getState().equals(STATE_OFF)){
            return Result.error(500, "空调已开机");
        }


        //设置默认开机状态
        airCondition.setState(STATE_ON);
        airCondition.setFee(new BigDecimal("0.0"));
        airCondition.setTargetTemperature(TEMPT_DEFAULT);
        airCondition.setSpeed(SPEED_DEFAULT);
        //写入开机记录
        //airConditionLogMapper.insert(new AirConditionLog(acId, STATE_ON, systemClock));

        //设置初始温度
        airCondition.setOriginTemperature(new BigDecimal(Double.toString(cTempt)));
        //设置当前温度
        airCondition.setCurrentTemperature(new BigDecimal(Double.toString(cTempt)));
        List<AirCondition> airConditionList1=airConditionList.getAirConditionList();
        airConditionList1.set(acId,airCondition);
        airConditionList.setAirConditionList(airConditionList1);
        //设置空调到达目标温度位
        reachTarget.set(airCondition.getAcId(), NO_REACH_TARGET);
        /*封装并发送请求*/
        WaitObject waitObject = new WaitObject(acId, SPEED_DEFAULT,  0);
        schedule(waitObject);
        return Result.ok(waitObject);
    }


    /**
     * @about: 接收空调关机请求，处理关机请求  by zhicheng lee
     */
    @ApiOperation(value = "关机请求")
    @GetMapping("/ac/shutdown")
    public Result<?> shutdown(@RequestParam Integer acId) {

        AirCondition airCondition = airConditionList.getAirCondition(acId);

        if (!state.equals(STATE_RUNNING)){
            return Result.error(500, "系统未运行!!");
        }
        //检查空调是否已经关机
        if (airCondition.getState().equals(STATE_OFF)){
            return Result.error(500,"空调已经关机");
        }
        /*检查ServeQueue中是否有请求；如果有，设置state为0，即空闲，同时将数据写入数据库*/
        for (ServeObject value : serveQueue.getServeObjectList()) {
            if (value.getState().equals(SERVE_RUNNING) && value.getAcId().equals(acId)) {
                //退出服务队列之前，设置属性
                /*
                * 暂时设置，为方便测试使用 ， endTime = startTime + serveTime
                *
                * */
                value.setEndTime(systemClock);//设置退出服务队列的时间
                //存入数据库
                serveMapper.insert(value);
                //设置ServeObject的状态
                value.setState(SERVE_IDLE);
                break;
            }
        }

        /*检查WaitQueue中是否有请求；如果有，删除该请求*/
        waitQueue.delete(acId);


        /*设置空调state*/
        //AirCondition airCondition=airConditionList.getAirConditionList().get(acId);


        airCondition.setState(STATE_OFF);

        //写入关机记录
        airConditionLogMapper.insert(new AirConditionLog(acId, STATE_OFF, systemClock));

        return Result.ok(airCondition);
    }


    /**
     * @about: 接收调节风速、温度请求，处理请求
     * deWind、deTemp 为 目标风速、目标温度
     * todo: 参数如果未被设置，前端使用当前空调的参数，
     */
    @ApiOperation(value = "调节风速、温度请求")
    @GetMapping("/ac/adjust")
    public Result<?> adjust(@RequestParam Integer acId, @RequestParam Integer deWind, @RequestParam Double deTemp) {
        if (!state.equals(STATE_RUNNING)){
            return Result.error(500, "系统未运行!!");
        }
        /*检查空调是否开机*/
        AirCondition airCondition = airConditionList.getAirCondition(acId);
        if(airCondition.getState().equals(STATE_OFF)){
            return Result.error(500,"该空调未开机！");
        }
        if (deTemp != -1 ){
            BigDecimal deTempB = new BigDecimal(Double.toString(deTemp));
            //判断温度是否在温度控制范围内
            if (deTempB.compareTo(TEMPT_LOW_LIMIT) < 0 || deTempB.compareTo(TEMPT_HIGH_LIMIT) > 0){
                return Result.error(500, "目标温度不在温度控制范围内");
            }
            airCondition.setTargetTemperature(deTempB);
        }
        if (deWind != -1){
//            WaitObject waitObject = new WaitObject(acId, deWind, airCondition.getTargetTemperature(), 0);
            WaitObject waitObject = new WaitObject(acId, deWind, 0);
            schedule(waitObject);
        }
        return Result.ok("设置参数成功");
    }

    @ApiOperation(value = "获取单个空调状态")
    @GetMapping("/ac/state")
    public Result<?> getAcState(@RequestParam Integer acId){
        return Result.ok(airConditionList.getAirCondition(acId));
    }


    /* **********************************空调管理员相关*************************************/


    /**
     * @about: 处理中央空调开启请求
     */
    @ApiOperation(value = "开启中央空调")
    @GetMapping("/admin/start")
    public Result<?>  turnOnSystem(@RequestParam Integer numOfServeObject){
        if (state.equals(STATE_OFF)){
            state = STATE_ON;
            serveQueue = new ServeQueue();
            waitQueue = new WaitQueue();
            airConditionList = new AirConditionList();
            customerList=new ArrayList<>(); //开启中央空调后 才可以办理入住
            reachTarget = new ArrayList<>(NUM_OF_AIR_CONDITION);
            for (int i = 0; i < NUM_OF_AIR_CONDITION; i++) {
                reachTarget.add(NO_REACH_TARGET);//一开始都是0
            }
            //服务对象初始化,传入服务对象的个数
            serveQueue.init(numOfServeObject);
            return Result.ok(serveQueue.getServeObjectList());
        }else{
            return Result.error(500, "中央空调已开机");
        }
    }

    /**
     * @about: 设置空调初始工作参数
     * @author ssyl
     */
    @ApiOperation(value = "设置中央空调默认模式")
    @GetMapping("/admin/mode")
    public Result<?> setMode(@RequestParam Integer defaultMode,@RequestParam Integer defaultSpeed, @RequestParam Double temptHighLimit, @RequestParam Double temptLowLimit, @RequestParam Double defaultTempt,
                               @RequestParam Double feeRateH, @RequestParam Double feeRateM, @RequestParam Double feeRateL) {
        if (state.equals(STATE_ON)){
            //系统默认参数设置
            MODE_DEFAULT = defaultMode;
            SPEED_DEFAULT = defaultSpeed;
            TEMPT_DEFAULT = new BigDecimal(Double.toString(defaultTempt));
            TEMPT_HIGH_LIMIT = new BigDecimal(Double.toString(temptHighLimit));
            TEMPT_LOW_LIMIT = new BigDecimal(Double.toString(temptLowLimit));
            feeMap.put(SPEED_H, new BigDecimal(Double.toString(feeRateH)));
            feeMap.put(SPEED_M, new BigDecimal(Double.toString(feeRateM)));
            feeMap.put(Constant.SPEED_L, new BigDecimal(Double.toString(feeRateL)));
            temptMap.put(SPEED_L, new BigDecimal("0.4"));
            temptMap.put(SPEED_M, new BigDecimal("0.5"));
            temptMap.put(SPEED_H, new BigDecimal("0.6"));

            //设置服务对象默认模式
//            serveQueue.setDefaultPara(defaultMode, defaultSpeed, feeMap.get(defaultSpeed),
//                                    TEMPT_DEFAULT, TEMPT_DEFAULT, defaultMode.equals(MODE_R)?TEMPT_LOW_LIMIT:TEMPT_HIGH_LIMIT,new BigDecimal("0.0"));
            serveQueue.setDefaultPara(defaultMode, defaultSpeed, feeMap.get(defaultSpeed),
                    TEMPT_DEFAULT, TEMPT_DEFAULT, TEMPT_DEFAULT,new BigDecimal("0.0"));
            //设置空调默认状态
//            airConditionList.setDefaultPara(TEMPT_DEFAULT, defaultMode, defaultMode.equals(MODE_R)?TEMPT_LOW_LIMIT:TEMPT_HIGH_LIMIT, defaultSpeed, STATE_OFF, new BigDecimal("0.0"));
            airConditionList.setDefaultPara(TEMPT_DEFAULT, defaultMode, TEMPT_DEFAULT, defaultSpeed, STATE_OFF, new BigDecimal("0.0"));
            return Result.ok(serveQueue.getServeObjectList());
        }else if (state.equals(STATE_OFF)){

            return Result.error(500, "系统未开机!!!");

        }else {
            return Result.error(500, "系统已经运行!!!");
        }
    }

    /**
     *
     *
     * @author ssyl
     */
    @ApiOperation(value = "空调开始运行")
    @GetMapping("/admin/run")
    public Result<?> run(){
        if (state.equals(STATE_ON)){
            state = STATE_RUNNING;
            serveQueue.setReady();
            return Result.ok(serveQueue.getServeObjectList());
        }else {
            return Result.error(500, "空调已运行或尚未开机");
        }
    }

    /**
     *
     * @about: 查看空调运行状态
     * @period: 1 min
     * @author ssyl
     */
    @ApiOperation(value = "获取空调运行状态")
    @GetMapping("/admin/state")
    public Result<?> getState(){
        if (!state.equals(STATE_RUNNING)){
            return Result.error(500, "空调未运行");
        }
        //更新系统时间
        systemClock.setMinutes(systemClock.getMinutes()+1);
        //更新等待队列的等待时长
        waitQueue.update();

        //对于达到目标温度后未被服务的空调,每分钟温度变化0.5
        for (AirCondition a:airConditionList.getAirConditionList()) {
            //处于待机状态
            if (a.getState().equals(STATE_ON) && reachTarget.get(a.getAcId()).equals(REACH_TARGET)){
                //温度变化0.5度
                if (a.getMode().equals(MODE_H)){
                    //处于制热模式下，当前温度 > 原始温度, 不能小于原始温度
                    a.setCurrentTemperature(a.getCurrentTemperature().subtract(new BigDecimal("0.5")).compareTo(a.getOriginTemperature()) > 0?a.getCurrentTemperature().subtract(new BigDecimal("0.5")):a.getOriginTemperature());
                }else if (a.getMode().equals(MODE_R)){
                    //处于制冷模式下,当前温度 < 原始温度， 不能大于原始温度
                    a.setCurrentTemperature(a.getCurrentTemperature().add(new BigDecimal("0.5")).compareTo(a.getOriginTemperature()) < 0?a.getCurrentTemperature().add(new BigDecimal("0.5")):a.getOriginTemperature());
                }
                //当前温度和目标温度差大于1度
                if (a.getCurrentTemperature().subtract(a.getTargetTemperature()).abs().compareTo(new BigDecimal("1.0")) >= 0){
                    reachTarget.set(a.getAcId(), NO_REACH_TARGET);
                    WaitObject waitObject = new WaitObject();
                    waitObject.setAcId(a.getAcId());
                    waitObject.setSpeed(a.getSpeed());
                    waitObject.setTimeToWait(TIME_TO_WAIT);
                    waitQueue.insert(waitObject);
                }
            }

            //对于关机的空调,温度变化0.5度
            if (a.getState().equals(STATE_OFF)){
                //温度变化0.5度
                if (a.getMode().equals(MODE_H)){
                    //处于制热模式下，当前温度 > 原始温度, 不能小于原始温度
                    a.setCurrentTemperature(a.getCurrentTemperature().subtract(new BigDecimal("0.5")).compareTo(a.getOriginTemperature()) > 0?a.getCurrentTemperature().subtract(new BigDecimal("0.5")):a.getOriginTemperature());
                }else if (a.getMode().equals(MODE_R)){
                    //处于制冷模式下,当前温度 < 原始温度， 不能大于原始温度
                    a.setCurrentTemperature(a.getCurrentTemperature().add(new BigDecimal("0.5")).compareTo(a.getOriginTemperature()) < 0?a.getCurrentTemperature().add(new BigDecimal("0.5")):a.getOriginTemperature());
                }
            }
        }
        //正在接收服务的空调
        for (ServeObject s:serveQueue.getServeObjectList()) {
            if (s.getState().equals(SERVE_RUNNING)){
                //更新对应房间的状态
                AirCondition airCondition = airConditionList.getAirCondition(s.getAcId());
                //更新温度
                BigDecimal currentTempt = s.getMode().equals(MODE_H)?s.getCurrentTemp().add(temptMap.get(s.getSpeed())):s.getCurrentTemp().subtract(temptMap.get(s.getSpeed()));
                if (s.getMode().equals(MODE_H)){
                    //最高只能达到目标温度，不能超过目标温度
                    if (currentTempt.compareTo(airCondition.getTargetTemperature()) > 0){
                        s.setCurrentTemp(airCondition.getTargetTemperature());
                    }else{
                        s.setCurrentTemp(currentTempt);
                    }
                }else if (s.getMode().equals(MODE_R)){
                    //达到目标温度
                    if (currentTempt.compareTo(airCondition.getTargetTemperature()) < 0){
                        s.setCurrentTemp(airCondition.getTargetTemperature());
                    }else{
                        s.setCurrentTemp(currentTempt);
                    }
                }
                //更新服务时长
                s.setServeTime(s.getServeTime() + 1);
                //更新费用
                s.setFee(s.getFee().add(feeMap.get(s.getSpeed())));
                //空调记录总费用
                airCondition.setFee(airCondition.getFee().add(feeMap.get(s.getSpeed())));
                airCondition.setCurrentTemperature(s.getCurrentTemp());
                //判断是否达到目标温度
                if (airCondition.getCurrentTemperature().equals(airCondition.getTargetTemperature())){
                    //出服务队列
                    s.setEndTime(systemClock);
                    //记录保存到数据库
                    serveMapper.insert(s);
                    //服务对象空闲
                    s.setState(SERVE_IDLE);

                    reachTarget.set(airCondition.getAcId(),REACH_TARGET);
                    //空调未被服务
                    airCondition.setState(STATE_ON);
                }
            }
        }
//        //更新等待队列的等待时长
//        waitQueue.update();
        //调度等待队列
        schedule();
        //返回所有空调的状态
        Env env = new Env();
        env.setAirConditionList(airConditionList);
        env.setServeQueue(serveQueue);
        env.setWaitQueue(waitQueue);
        return Result.ok(env);
    }

    /**
     *
     * @about: 关闭中央空调
     *
     * @author ssyl
     */
    @ApiOperation(value = "关闭中央空调")
    @GetMapping("/admin/off")
    public Result<?> turnOff(){
        state = STATE_OFF;
        airConditionList.setOff();
        serveQueue.setReady();
        return Result.ok();
    }


    /* ************************************前台服务员**************************************/

    /**
     * @about 创建账单及详单  by zhicheng lee
     */
    public BillsWithDetails createBillsWithDetails(@RequestParam Integer acId) {
        /*查找客户入住、退房时间*/
        Customer customer = null;
        for (Customer value : customerList) {//遍历所有客户，查找acId对应的客户
            if (value.getAcId() == acId) {
                customer = value;
                break;
            }
        }
        if (customer == null) {//若不存在该客户
            return null;
        }

        long startTime = customer.getInTime().getTime();
        long endTime = customer.getOutTime().getTime();
        Date startTimeDate=new Date(startTime);
        Date endTimeDate=new Date(endTime);

        QueryWrapper<ServeObject> queryWrapper = new QueryWrapper<ServeObject>();
        queryWrapper.eq("ac_id", acId).ge("start_time", startTimeDate).le("end_time", endTimeDate);
        List<ServeObject> serveObjectList = serveMapper.selectList(queryWrapper);

        /*组装详单*/
        //创建空账单及详单
        BillsWithDetails billsWithDetails = new BillsWithDetails();
        List<DetailObject> detailObjectList = new ArrayList<>();
        BigDecimal allFee = new BigDecimal("0.0");           //总费用
        BigDecimal allConsumption = new BigDecimal("0.0");    //总耗电

        //组装详单
        for (ServeObject value : serveObjectList) {
            DetailObject detailObject = new DetailObject();

            /*账单信息*/
            allConsumption = allConsumption.add(value.getFee());
            allFee = allFee.add(value.getFee());

            detailObject.setAcId(value.getAcId());
            detailObject.setSpeed(value.getSpeed());
            detailObject.setMode(value.getMode());
            detailObject.setStartTime(value.getStartTime());
            detailObject.setEndTime(value.getEndTime());
            detailObject.setStartTempt(value.getStartTemp());//设置开始温度
            detailObject.setEndTempt(value.getCurrentTemp());
            detailObject.setConsumption(value.getFee());
            detailObject.setFee(value.getFee());

            /*添加详单项*/
            detailObjectList.add(detailObject);
        }

        /*添加详单列表*/
        billsWithDetails.setDetailObjectList(detailObjectList);

        /*添加账单项*/
        billsWithDetails.setCustomerId(customer.getCustomerId());
        billsWithDetails.setName(customer.getName());
        billsWithDetails.setAcId(customer.getAcId());
        billsWithDetails.setStartTime(customer.getInTime());
        billsWithDetails.setEndTime(customer.getOutTime());
        billsWithDetails.setAllConsumption(allConsumption);
        billsWithDetails.setAllFee(allFee);

        return billsWithDetails;
    }

    /**
     * @about 客户入住  by zhicheng lee
     */
    @ApiOperation(value = "客户入住")
    @GetMapping("/front/checkin")
    public Result<?> checkIn(@RequestParam String name) {

        /*判断是否有空房*/
        Integer freeNum = -1;   //值为id最小的空房
        Boolean[] is_free = new Boolean[NUM_OF_AIR_CONDITION];  //用来检查对应id的房间是否空闲
        for (int i = 0; i < NUM_OF_AIR_CONDITION; i++) {    //初始化
            is_free[i] = true;
        }
        for (Customer value : customerList) {   //遍历所有房间，检查是否空闲
            is_free[value.getAcId()] = false;
        }

        for (int i = 0; i < NUM_OF_AIR_CONDITION; i++) {    //找到最小id的空房
            if (is_free[i]) {
                freeNum = i;
                break;
            }
        }

        if (freeNum == -1) {
            return Result.error("无空房！");
        }

        /*创建客户，给客户分配房间，初始化客户信息*/
        //设置customer的id
        Integer cusId=0;
        for(Customer value:customerList){
            if(value.getCustomerId()>=cusId){
                cusId=value.getCustomerId()+1;
            }
        }

        /*创建客户，分配房间，并初始化信息*/
        Date date=new Date(systemClock.getTime()-1000);
        Customer customer = new Customer();
        customer.setAcId(cusId);
        customer.setInTime(date);
        customer.setOutTime(date);
        customer.setName(name);
        customer.setAcId(freeNum);

        customerList.add(customer);

        /*初始化空调的fee、scheduleTime、useTime*/
        AirCondition airCondition = airConditionList.getAirConditionList().get(freeNum);
        List<AirCondition> airConditionList1=airConditionList.getAirConditionList();

        airCondition.setFee(new BigDecimal("0.0"));
//        airCondition.setScheduleTime(0);
//        airCondition.setUseTime(0);

        airConditionList1.set(freeNum, airCondition);
        airConditionList.setAirConditionList(airConditionList1);

        return Result.ok(customer);
    }

    /**
     * @about 客户退房  by zhicheng lee
     */
    @ApiOperation(value = "客户退房")
    @GetMapping("/front/checkout")
    public Result<?> checkOut(@RequestParam Integer acId) {
        Boolean flag=true;  //标识客户是否存在
        for (Customer value : customerList) {
            if (value.getAcId() == acId) {
//                value.setOutTime(systemClock);
                flag=false;
                break;
            }
        }

        if(flag)
            return Result.error("客户不存在!");

        /*检查ServeQueue中是否有请求；如果有，设置state为0，即空闲，同时将数据写入数据库*/
        for (ServeObject value : serveQueue.getServeObjectList()) {
            if (value.getAcId() == acId) {
                //退出服务队列之前，设置属性
                value.setEndTime(systemClock);//设置退出服务队列的时间

                //计算结束时温度、耗电量、费用
                BigDecimal duration = new BigDecimal(Double.toString((value.getEndTime().getTime() - value.getStartTime().getTime()) * 1.0 / 60000));
                BigDecimal fee = duration.multiply(new BigDecimal(value.getFeeRate().toString())); //入住时长
                BigDecimal endTempt=new BigDecimal(value.getStartTemp().toString());    //结束温度


                if (value.getSpeed().equals(SPEED_L)) {
                    duration=duration.multiply(new BigDecimal(value.getMode().equals(MODE_H)?"0.4":"-0.4"));//转换时间为温度差
                    endTempt=endTempt.add(duration);
                } else if (value.getSpeed().equals(SPEED_M)) {
                    duration=duration.multiply(new BigDecimal(value.getMode().equals(MODE_H)?"0.5":"-0.5"));
                    endTempt = endTempt.add(duration);
                } else {
                    duration=duration.multiply(new BigDecimal(value.getMode().equals(MODE_H)?"0.6":"-0.6"));
                    endTempt = endTempt.add(duration);
                }

                value.setFee(fee);
                value.setCurrentTemp(endTempt);

                //存入数据库
                serveMapper.insert(value);

                //设置ServeObject的状态
                List<ServeObject> serveObjectList1=serveQueue.getServeObjectList();
                value.setState(SERVE_IDLE);
                serveObjectList1.set(value.getServeId(),value);
                break;
            }
        }

        /*检查WaitQueue中是否有请求；如果有，删除该请求*/
        waitQueue.delete(acId);

        /*设置客户退房时间*/
        for (Customer value : customerList) {
            if (value.getAcId() == acId) {
                value.setOutTime(new Date(systemClock.getTime()+2000));
                break;
            }
        }

        /*设置空调state*/
        AirCondition airCondition = airConditionList.getAirConditionList().get(acId);
        List<AirCondition> airConditionList1=airConditionList.getAirConditionList();

        airCondition.setState(0);

        airConditionList1.set(acId, airCondition);
        airConditionList.setAirConditionList(airConditionList1);

        /*打印账单、详单*/
        BillsWithDetails billsWithDetails = createBillsWithDetails(acId);

        /*删除客户*/
        for (Customer value : customerList) {
            if (value.getAcId() == acId) {
                customerList.remove(value);
                break;
            }
        }

        return Result.ok(billsWithDetails);
    }





    /* ************************************酒店经理*********************************************/
    /*
     * 调度次数计算：
     *   特殊情况：某个空调的旧情求被新请求替换，调度次数算1次,只计算了调度入队列
     *   空调关机也未被计算在内
     *
     * */

    /**
     *
     * @param type 报表类型: REPORT_DAY 日报表， REPORT_WEEK 周报表, REPORT_MONTH 月报表， REPORT_YEAR 年报表
     * @param date 报表开始日期
     *
     */
    @ApiOperation(value = "获取报表")
    @GetMapping("/manager/report")
    public Result<?> getReport(@RequestParam Integer type, @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date date){

        Date start_time = null;
        Date end_time = null;
        if (type.equals(REPORT_YEAR)){
            start_time = new Date(date.getYear(), 0, 0);
            end_time = new Date(date.getYear()+1, 0, 0);
        }else if (type.equals(REPORT_MONTH)){
            start_time = new Date(date.getYear(), date.getMonth(),1);
            end_time = new Date(date.getYear(), date.getMonth()+1,1);
        }else if(type.equals(REPORT_WEEK)){
            start_time = new Date(date.getYear(), date.getMonth(), date.getDate());
            end_time = new Date(date.getYear(), date.getMonth(), date.getDate()+7);
        }else{
            start_time = date;
            end_time = new Date(date.getYear(), date.getMonth(), date.getDate()+1);
        }
        System.out.println(date);
        System.out.println(start_time);
        System.out.println(end_time);
        Report report = new Report();
        report.setStart_time(start_time);
        report.setEnd_time(end_time);
        List<ReportItem> reportItems = report.getReportItemList();
        for (AirCondition a:airConditionList.getAirConditionList()) {
            ReportItem reportItem = new ReportItem();
            reportItem.setAcId(a.getAcId());
            //获取使用空调次数
            reportItem.setUseTimes(airConditionLogMapper.selectCount(new QueryWrapper<AirConditionLog>().ge("date", start_time)
                    .le("date", end_time).eq("ac_id", a.getAcId()))/2);
            //最常用目标温度
            reportItem.setCommonTargetTemperature(serveMapper.getCommonTargetTemp(a.getAcId(),start_time,end_time));
            //最常用风速
            reportItem.setCommonSpeed(serveMapper.getCommonSpeed(a.getAcId(),start_time,end_time));
            //到达目标温度次数
            reportItem.setReachTimes(serveMapper.getReachTargetTimes(a.getAcId(),start_time,end_time));
            //被调度次数
            reportItem.setScheduleTimes(scheduleLogMapper.selectCount(new QueryWrapper<ScheduleLog>().ge("date", start_time)
                    .le("date", end_time).eq("ac_id", a.getAcId())));
            //详单记录数
            reportItem.setDetailRecordTimes(serveMapper.getDetailCount(a.getAcId(),start_time,end_time));
            //总费用
            reportItem.setCost(serveMapper.getFee(a.getAcId(),start_time,end_time));

            reportItems.add(reportItem);
        }
        return Result.ok(report);
    }









    /* ****************************************调度************************************************/


    /**
     * 有空闲服务对象和从等待队列中调度处的等待对象
     * @param serveId 服务对象Id not null
     * @param waitObject 等待对象 not null
     */
    private void join(Integer serveId, WaitObject waitObject){
        AirCondition airCondition = airConditionList.getAirCondition(waitObject.getAcId());
        //检查空调是否被服务,未被服务，返回null;被服务,返回服务对象
        ServeObject oldServeObject = serveQueue.check(waitObject.getAcId());
        if(oldServeObject != null){//丢弃旧服务
            oldServeObject.setEndTime(systemClock);
            serveMapper.insert(oldServeObject);
            oldServeObject.setState(SERVE_IDLE);
        }
        ServeObject serveObject = serveQueue.getServeObject(serveId);
        //设置新分配的服务对象的状态
        serveObject.setAcId(waitObject.getAcId());
        serveObject.setState(SERVE_RUNNING);
        serveObject.setStartTime(new Date(systemClock.getTime()));
        serveObject.setMode(getMode(airCondition.getTargetTemperature()));
        serveObject.setEndTime(null);
        serveObject.setStartTemp(airCondition.getCurrentTemperature());
        serveObject.setCurrentTemp(airCondition.getCurrentTemperature());
        serveObject.setSpeed(waitObject.getSpeed());
        serveObject.setFeeRate(feeMap.get(waitObject.getSpeed()));
        serveObject.setFee(new BigDecimal("0.0"));
        serveObject.setServeTime(0);
        serveObject.setTargetTemp(airCondition.getTargetTemperature());
        //设置空调的状态
        airCondition.setState(STATE_RUNNING);
        airCondition.setMode(serveObject.getMode());
        airCondition.setSpeed(serveObject.getSpeed());
//        airCondition.setTargetTemperature(waitObject.getTargetTempt());
    }

    /**
     * 当有新的请求到来时,处理新请求
     * @param waitObject 等待对象 not null
     *
     * @author ssyl
     */
    //更新: 空调正在服务，旧服务对象直接替换新服务对象

    /*
     * 新请求到来时的调度规则:
     * 1、删除等待队列中已有的旧情求
     * 2、若服务队列有空闲服务对象，将等待对象直接加入服务队列
     * 3、如果服务队列没有空闲服务对象，比较此等待对象和服务队列中所有服务对象的优先级， 若比服务队列中某些服务对象优先级高，转4，；若和某些服务对象优先级相等，转5；比所有服务对象优先级低，转6
     * 4、用此等待对象替换服务队列中优先级低的服务对象，并将其加入到等待队列中，具体替换规则，转7
     * 5、直接插入等待队列，分配默认时间片
     * 6、直接插入等待队列，分配默认时间片
     * 7、选出所有优先级低于该等待对象的服务队列、从最低优先级的服务对象中选择出服务时长最长的服务对象替换
     */

    /*
     * 针对正在接收服务的空调的新请求的调度规则
     * 1、新请求比较优先级比服务队列中的某些服务对象优先级高，用新请求替换旧情求
     * 2、新请求置于等待队列中，等待调度，旧情求仍旧服务
     */
    private void schedule(WaitObject waitObject){
        //如果存在,删除等待队列中的旧请求
        waitQueue.delete(waitObject.getAcId());
        //有空闲的服务对象
        Integer serveId = serveQueue.hasIdle();
        if (serveId != -1){
            join(serveId, waitObject);
        }
        //需要调度
        else{
            //比较优先级
            Integer s = serveQueue.comparePriority(waitObject.getSpeed());
            System.out.println("优先级: " + s);
            //比服务对象优先级小
            if (s.equals(-1)){
                waitObject.setTimeToWait(TIME_TO_WAIT);
                waitQueue.insert(waitObject);
            }
            //与服务对象优先级相等
            if(s.equals(0)){
                //设置等待时长
                waitObject.setTimeToWait(TIME_TO_WAIT);
                waitQueue.insert(waitObject);
            }
            //比服务对象优先级高
            if (s.equals(1)){
                AirCondition airCondition = airConditionList.getAirCondition(waitObject.getAcId());
                //保存中风速的服务对象
                List<ServeObject> serveObjectM = new ArrayList<>();
                //保存低风速的服务对象
                List<ServeObject> serveObjectL = new ArrayList<>();
                for (ServeObject s1: serveQueue.getServeObjectList()) {
                    //如果旧情求在被服务，新请求被调度到服务队列
                    if (s1.getAcId().equals(waitObject.getAcId())) {
                        //旧情求提前结束，写入数据库
                        s1.setEndTime(systemClock);
                        serveMapper.insert(s1);
                        //新请求替换旧情求所在服务对象
                        s1.setServeTime(0);
                        s1.setFee(new BigDecimal("0.0"));
                        s1.setFeeRate(feeMap.get(waitObject.getSpeed()));
                        s1.setTargetTemp(airCondition.getTargetTemperature());
                        s1.setSpeed(waitObject.getSpeed());
                        s1.setStartTemp(airCondition.getCurrentTemperature());
                        s1.setCurrentTemp(airCondition.getCurrentTemperature());
                        s1.setEndTime(null);
                        //自动设置模式
                        s1.setMode(getMode(airCondition.getTargetTemperature()));
                        s1.setStartTime(new Date(systemClock.getTime()));
                        airCondition.setTargetTemperature(airCondition.getTargetTemperature());
                        airCondition.setSpeed(waitObject.getSpeed());
                        airCondition.setMode(s1.getMode());
                        return;
                    }else{
                        //选择出所有优先级低于传入的等待对象优先级的服务对象
                        if (s1.getSpeed() < waitObject.getSpeed() && s1.getSpeed().equals(SPEED_M)){
                            serveObjectM.add(s1);
                        }
                        if (s1.getSpeed() < waitObject.getSpeed() && s1.getSpeed().equals(SPEED_L)){
                            serveObjectL.add(s1);
                        }
                    }
                }

                ServeObject serveObject = null;
                //从低风速队列中选择
                if (! serveObjectL.isEmpty()){
                    serveObject = serveObjectL.get(0);
                    //选取服务时长最长的服务对象
                    for (ServeObject s2:serveObjectL) {
                        if (s2.getServeTime() > serveObject.getServeTime()){
                            serveObject = s2;
                        }
                    }
                } else if (! serveObjectM.isEmpty()){
                    serveObject = serveObjectM.get(0);
                    //选取服务时长最长的服务对象
                    for (ServeObject s2:serveObjectM) {
                        if (s2.getServeTime() > serveObject.getServeTime()){
                            serveObject = s2;
                        }
                    }
                }

                //选择出替换的服务对象
                if (serveObject != null){
                    WaitObject waitObjectNew = new WaitObject();
                    waitObjectNew.setAcId(serveObject.getAcId());
                    waitObjectNew.setSpeed(serveObject.getSpeed());
                    waitObjectNew.setTimeToWait(TIME_TO_WAIT);
//                    waitObjectNew.setTargetTempt(airConditionList.getAirCondition(serveObject.getAcId()).getTargetTemperature());
                    serveObject.setEndTime(systemClock);
                    serveMapper.insert(serveObject);
                    //写入调度出队列记录
                    scheduleLogMapper.insert(new ScheduleLog(serveObject.getAcId(),SCHEDULE_OUT, serveObject.getServeId(),systemClock));
                    //设置对应的空调状态为待机，未被服务
                    airConditionList.setAirConditionOn(waitObjectNew.getAcId());
                    //等待队列中不存在该空调的请求，插入等待队列， 若存在则丢弃该请求，旧情求丢弃
                    if (! waitQueue.check(waitObjectNew.getAcId())){
                        waitQueue.insert(waitObjectNew);
                    }
                    //设置新服务对象的状态
                    serveObject.setAcId(waitObject.getAcId());
                    serveObject.setMode(getMode(airCondition.getTargetTemperature()));
                    //设置服务对象
                    serveObject.setSpeed(waitObject.getSpeed());
                    serveObject.setState(SERVE_RUNNING);
                    serveObject.setStartTemp(airCondition.getCurrentTemperature());
                    serveObject.setStartTime(new Date(systemClock.getTime()));
                    serveObject.setEndTime(null);
                    serveObject.setFeeRate(feeMap.get(waitObject.getSpeed()));
                    serveObject.setFee(new BigDecimal("0.0"));
                    serveObject.setServeTime(0);
                    serveObject.setCurrentTemp(airCondition.getCurrentTemperature());
                    serveObject.setTargetTemp(airCondition.getTargetTemperature());
                    //设置空调
                    airCondition.setState(STATE_RUNNING);
//                    airCondition.setScheduleTime(airCondition.getScheduleTime()+1);
                    airCondition.setMode(serveObject.getMode());
                    airCondition.setSpeed(waitObject.getSpeed());
//                    airCondition.setTargetTemperature(waitObject.getTargetTempt());
                    //写入调度入队列记录
                    scheduleLogMapper.insert(new ScheduleLog(serveObject.getAcId(),SCHEDULE_IN, serveObject.getServeId(),systemClock));
                }
            }
        }
    }

    /**
     * 若有空闲直接替换包括等待时长\<=0的等待对象和等待时长\>0的等待对象
     * 若没有空闲,对于等待时长<=0使用时间片调度服务队列中同等级或低等级的服务对象，并给替换下来的服务对象设置同样的等待时长
     * 没有空闲、替换结束的标志：无更多满足的等待时长<=0的等待对象或 无法替换任何服务时长>0的服务对象
     */
    private void schedule(){
        List<WaitObject> failChange = new ArrayList<>();
        WaitObject waitObject = waitQueue.schedule();
        while (waitObject != null){
            Integer serveId = serveQueue.hasIdle();
            //存在空闲服务对象
            if (serveId != -1){
                join(serveId, waitObject);
//                waitQueue.delete(waitObject);
            }else{
                //无空闲服务对象

                //优先级比服务队列中的某些服务对象优先级高
                if(serveQueue.comparePriority(waitObject.getSpeed()) > 0){
                    System.out.println("替换高等级");
                    AirCondition airCondition = airConditionList.getAirCondition(waitObject.getAcId());
                    //保存中风速的服务对象
                    List<ServeObject> serveObjectM = new ArrayList<>();
                    //保存低风速的服务对象
                    List<ServeObject> serveObjectL = new ArrayList<>();
                    for (ServeObject s1: serveQueue.getServeObjectList()) {
                        //如果旧情求在被服务，新请求被调度到服务队列
                        if (s1.getAcId().equals(waitObject.getAcId())) {
                            //旧情求提前结束，写入数据库
                            s1.setEndTime(systemClock);
                            serveMapper.insert(s1);
                            //新请求替换旧情求所在服务对象
                            s1.setServeTime(0);
                            s1.setFee(new BigDecimal("0.0"));
                            s1.setFeeRate(feeMap.get(waitObject.getSpeed()));
                            s1.setTargetTemp(airCondition.getTargetTemperature());
                            s1.setSpeed(waitObject.getSpeed());
                            s1.setStartTemp(airCondition.getCurrentTemperature());
                            s1.setCurrentTemp(airCondition.getCurrentTemperature());
                            s1.setEndTime(null);
                            //自动设置模式
                            s1.setMode(getMode(airCondition.getTargetTemperature()));
                            s1.setStartTime(new Date(systemClock.getTime()));
//                            airCondition.setTargetTemperature();
                            airCondition.setSpeed(waitObject.getSpeed());
                            airCondition.setMode(s1.getMode());
                            return;
                        }else{
                            //选择出所有优先级低于传入的等待对象优先级的服务对象
                            if (s1.getSpeed() < waitObject.getSpeed() && s1.getSpeed().equals(SPEED_M)){
                                serveObjectM.add(s1);
                            }
                            if (s1.getSpeed() < waitObject.getSpeed() && s1.getSpeed().equals(SPEED_L)){
                                serveObjectL.add(s1);
                            }
                        }
                    }
                    ServeObject serveObject = null;
                    //从低风速队列中选择
                    if (! serveObjectL.isEmpty()){
                        serveObject = serveObjectL.get(0);
                        //选取服务时长最长的服务对象
                        for (ServeObject s2:serveObjectL) {
                            if (s2.getServeTime() > serveObject.getServeTime()){
                                serveObject = s2;
                            }
                        }
                    } else if (! serveObjectM.isEmpty()){
                        serveObject = serveObjectM.get(0);
                        //选取服务时长最长的服务对象
                        for (ServeObject s2:serveObjectM) {
                            if (s2.getServeTime() > serveObject.getServeTime()){
                                serveObject = s2;
                            }
                        }
                    }
                    //选择出替换的服务对象
                    if (serveObject != null){
                        WaitObject waitObjectNew = new WaitObject();
                        waitObjectNew.setAcId(serveObject.getAcId());
                        waitObjectNew.setSpeed(serveObject.getSpeed());
                        waitObjectNew.setTimeToWait(TIME_TO_WAIT);
//                        waitObjectNew.setTargetTempt(airConditionList.getAirCondition(serveObject.getAcId()).getTargetTemperature());
                        serveObject.setEndTime(systemClock);
                        serveMapper.insert(serveObject);
                        //写入调度出队列记录
                        scheduleLogMapper.insert(new ScheduleLog(serveObject.getAcId(),SCHEDULE_OUT, serveObject.getServeId(),systemClock));
                        //设置对应的空调状态为开机，未被服务
                        airConditionList.setAirConditionOn(waitObjectNew.getAcId());
                        //等待队列中不存在该空调的请求，插入等待队列， 若存在则丢弃该请求，旧情求丢弃
                        if (! waitQueue.check(waitObjectNew.getAcId())){
                            waitQueue.insert(waitObjectNew);
                        }
                        //设置新服务对象的状态
                        serveObject.setAcId(waitObject.getAcId());
                        serveObject.setMode(getMode(airCondition.getTargetTemperature()));
                        //设置服务对象
                        serveObject.setSpeed(waitObject.getSpeed());
                        serveObject.setState(SERVE_RUNNING);
                        serveObject.setStartTemp(airCondition.getCurrentTemperature());
                        serveObject.setStartTime(new Date(systemClock.getTime()));
                        serveObject.setEndTime(null);
                        serveObject.setFeeRate(feeMap.get(waitObject.getSpeed()));
                        serveObject.setFee(new BigDecimal("0.0"));
                        serveObject.setServeTime(0);
                        serveObject.setCurrentTemp(airCondition.getCurrentTemperature());
                        serveObject.setTargetTemp(airCondition.getTargetTemperature());
                        //设置空调
                        airCondition.setState(STATE_RUNNING);
//                    airCondition.setScheduleTime(airCondition.getScheduleTime()+1);
                        airCondition.setMode(serveObject.getMode());
                        airCondition.setSpeed(waitObject.getSpeed());
//                        airCondition.setTargetTemperature(waitObject.getTargetTempt());
                        //写入调度入队列记录
                        scheduleLogMapper.insert(new ScheduleLog(serveObject.getAcId(),SCHEDULE_IN, serveObject.getServeId(),systemClock));
//                            waitQueue.delete(waitObject);
                    }
                }else if (serveQueue.comparePriority(waitObject.getSpeed()) == 0 && waitObject.getTimeToWait() <= 0) {
                    System.out.println("替换同等级");
                    //优先级比服务队列中的某些服务对象优先级相等
                    ServeObject serveObject = null;
                    for (ServeObject s : serveQueue.getServeObjectList()) {
                        if (s.getSpeed().equals(waitObject.getSpeed()) && s.getServeTime() != 0) {
                            serveObject = s;
                        }
                    }
                    if (serveObject != null) {
                        for (ServeObject s : serveQueue.getServeObjectList()) {
                            if (serveObject.getSpeed().equals(s.getSpeed()) && serveObject.getServeTime() < s.getServeTime()) {
                                serveObject = s;
                            }
                        }
                        //替换
                        AirCondition airCondition = airConditionList.getAirCondition(waitObject.getAcId());
                        WaitObject waitObjectNew = new WaitObject();
                        waitObjectNew.setAcId(serveObject.getAcId());
                        waitObjectNew.setSpeed(serveObject.getSpeed());
                        waitObjectNew.setTimeToWait(TIME_TO_WAIT);
//                        waitObjectNew.setTargetTempt(airConditionList.getAirCondition(serveObject.getAcId()).getTargetTemperature());
                        serveObject.setEndTime(systemClock);
                        serveMapper.insert(serveObject);
                        //写入调度出队列记录
                        scheduleLogMapper.insert(new ScheduleLog(serveObject.getAcId(), SCHEDULE_OUT, serveObject.getServeId(), systemClock));
                        //设置对应的空调状态为开机，未被服务
                        airConditionList.setAirConditionOn(waitObjectNew.getAcId());
                        //等待队列中不存在该空调的请求，插入等待队列， 若存在则丢弃该请求，旧情求丢弃
                        if (!waitQueue.check(waitObjectNew.getAcId())) {
                            waitQueue.insert(waitObjectNew);
                        }
                        //设置新服务对象的状态
                        serveObject.setAcId(waitObject.getAcId());
                        serveObject.setMode(getMode(airCondition.getTargetTemperature()));
                        //设置服务对象
                        serveObject.setSpeed(waitObject.getSpeed());
                        serveObject.setState(SERVE_RUNNING);
                        serveObject.setStartTemp(airCondition.getCurrentTemperature());
                        serveObject.setStartTime(new Date(systemClock.getTime()));
                        serveObject.setEndTime(null);
                        serveObject.setFeeRate(feeMap.get(waitObject.getSpeed()));
                        serveObject.setFee(new BigDecimal("0.0"));
                        serveObject.setServeTime(0);
                        serveObject.setCurrentTemp(airCondition.getCurrentTemperature());
                        serveObject.setTargetTemp(airCondition.getTargetTemperature());
                        //设置空调
                        airCondition.setState(STATE_RUNNING);
//                    airCondition.setScheduleTime(airCondition.getScheduleTime()+1);
                        airCondition.setMode(serveObject.getMode());
                        airCondition.setSpeed(waitObject.getSpeed());
//                        airCondition.setTargetTemperature(waitObject.getTargetTempt());
                        //写入调度入队列记录
                        scheduleLogMapper.insert(new ScheduleLog(serveObject.getAcId(), SCHEDULE_IN, serveObject.getServeId(), systemClock));
                    }
                }else{
                    //将未替换成功的等待对象置入一个列表，等待插回
                    failChange.add(waitObject);
                }
            }
            System.out.println("等待队列调度");
            waitObject = waitQueue.schedule();
        }
        waitQueue.insert(failChange);
    }
}


