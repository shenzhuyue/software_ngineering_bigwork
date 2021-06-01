package cn.edu.bupt.aircondition.Queue;

import cn.edu.bupt.aircondition.entity.WaitObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QueueTest {
    @Test
    public void test(){
        List<Integer> queue = new ArrayList<>();
        queue.add(5);
        queue.add(4);
        queue.add(1);
        Iterator<Integer> iterator = queue.iterator();
        while (iterator.hasNext()){
            Integer integer = iterator.next();
            integer = integer - 1;
        }


    }
}
