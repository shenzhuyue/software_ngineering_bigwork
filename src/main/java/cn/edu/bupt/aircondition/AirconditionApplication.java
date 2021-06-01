package cn.edu.bupt.aircondition;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(value = "cn.edu.bupt.aircondition.mapper")
public class AirconditionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AirconditionApplication.class, args);
    }

}
