package cn.edu.bupt.aircondition.controller;



import cn.edu.bupt.aircondition.entity.ServeQueue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

@SpringBootTest
public class SystemControllerTest {

    @Autowired
    SystemController systemController;
    @Test
    public void turnOnSystemTest(){
        systemController.turnOnSystem(3);
    }
}
