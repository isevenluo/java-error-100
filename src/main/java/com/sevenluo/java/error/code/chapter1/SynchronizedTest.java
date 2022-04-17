package com.sevenluo.java.error.code.chapter1;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author: sevenluo
 * @date: 2022/04/14 08:13
 * @description: 锁测试
 */
@RestController
@RequestMapping("synchronized")
@Slf4j
public class SynchronizedTest {


    @GetMapping("wrong")
    public int wrong(@RequestParam(value = "count", defaultValue = "1000000") int count) {
        Data.reset();
        //多线程循环一定次数调用Data类不同实例的wrong方法
        IntStream.rangeClosed(1, count).parallel().forEach(i -> new Data().wrong());
        return Data.getCounter();
    }



    private List<Integer> data = new ArrayList<>();

    //不涉及共享资源的慢方法
    private void slow() {
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
        }
    }

    //错误的加锁方法
    @GetMapping("/lock/wrong")
    public int wrong() {
        long begin = System.currentTimeMillis();
        IntStream.rangeClosed(1, 1000).parallel().forEach(i -> {
            //加锁粒度太粗了
            synchronized (this) {
                slow();
                data.add(i);
            }
        });
        log.info("took:{}", System.currentTimeMillis() - begin);
        return data.size();
    }

    //正确的加锁方法
    @GetMapping("/lock/right")
    public int right() {
        long begin = System.currentTimeMillis();
        IntStream.rangeClosed(1, 1000).parallel().forEach(i -> {
            slow();
            //只对List加锁
            synchronized (data) {
                data.add(i);
            }
        });
        log.info("took:{}", System.currentTimeMillis() - begin);
        return data.size();
    }

}


class Data {
    @Getter
    private static int counter = 0;

    public static int reset() {
        counter = 0;
        return counter;
    }

    /**
     * 非静态方法加锁只能保证同一个实例的 wrong 方法不被同时执行，但是不同实例可以执行各自实例的 wrong 方法。
     * 静态变量是所有实例共享，因此线程不安全
     */
    public synchronized void wrong() {
        counter++;
    }
}
