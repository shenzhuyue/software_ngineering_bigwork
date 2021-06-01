package cn.edu.bupt.aircondition.mapper;

import cn.edu.bupt.aircondition.entity.AirCondition;
import cn.edu.bupt.aircondition.entity.Customer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerMapper extends BaseMapper<Customer> {  //by zhicheng lee
}
