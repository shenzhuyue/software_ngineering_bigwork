package cn.edu.bupt.aircondition.mapper;

import cn.edu.bupt.aircondition.entity.ServeObject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Date;


@Repository
public interface ServeMapper extends BaseMapper<ServeObject> {  //添加了extends BaseMapper<ServeObject>，其余代码没动 by zhicheng lee

    //获取最常用目标温度
    @Select("SELECT A.target_temp FROM (SELECT target_temp, SUM(serve_time) as max_serve_time FROM serve_object WHERE ac_id = #{acId} AND start_time >= #{startTime} AND end_time <= #{endTime} GROUP BY target_temp ORDER BY max_serve_time  DESC LIMIT 1) as A")
    Double getCommonTargetTemp(@Param("acId") Integer acId, @Param("startTime")Date startTime, @Param("endTime")Date endTime);
    //获取最常用风速
    @Select("SELECT A.speed FROM (SELECT speed, SUM(serve_time) as max_serve_time FROM serve_object WHERE ac_id = #{acId} AND start_time >= #{startTime} AND end_time <= #{endTime} GROUP BY speed ORDER BY max_serve_time  DESC LIMIT 1) as A")
    Integer getCommonSpeed(@Param("acId") Integer acId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    //达到目标温度的次数
    @Select("SELECT COUNT(*) FROM serve_object WHERE ac_id = #{acId} AND start_time >= #{startTime} AND end_time <= #{endTime} AND current_temp = target_temp")
    Integer getReachTargetTimes(@Param("acId") Integer acId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    //详单数
    @Select("SELECT COUNT(*) FROM serve_object WHERE ac_id = #{acId} AND start_time >= #{startTime} AND end_time <= #{endTime}")
    Integer getDetailCount(@Param("acId") Integer acId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
    //花费
    @Select("SELECT SUM(fee) FROM serve_object WHERE ac_id = #{acId} AND start_time >= #{startTime} AND end_time <= #{endTime}")
    Double getFee(@Param("acId") Integer acId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
