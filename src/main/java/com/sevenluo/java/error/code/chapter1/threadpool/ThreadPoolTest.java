package com.sevenluo.java.error.code.chapter1.threadpool;

import jodd.util.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * @author: sevenluo
 * @date: 2022/04/17 23:09
 * @description: 线程池常见错误用法 Demo
 */
@Slf4j
@RestController
@RequestMapping("threadpool")
public class ThreadPoolTest {

    /**
     * 线程池眼里剖析
     * @return
     * @throws InterruptedException
     */
    @GetMapping("right")
    public int right() throws InterruptedException {
        //使用一个计数器跟踪完成的任务数
        AtomicInteger atomicInteger = new AtomicInteger();
        //创建一个具有2个核心线程、5个最大线程，使用容量为10的ArrayBlockingQueue阻塞队列作为工作队列的线程池，使用默认的AbortPolicy拒绝策略
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
                2, 5,
                5, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadFactoryBuilder().withNameFormat("seven-threadpool-%d").get(),
                new ThreadPoolExecutor.AbortPolicy());
        // 线程池创建后启动所有核心线程，无需等待提交任务
        int start = threadPool.prestartAllCoreThreads();
        log.info("{} thread is started",start);
        // 线程池在空闲时回收核心线程
        threadPool.allowCoreThreadTimeOut(true);

        printStats(threadPool);
        //每隔1秒提交一次，一共提交20次任务
        IntStream.rangeClosed(1, 20).forEach(i -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int id = atomicInteger.incrementAndGet();
            try {
                threadPool.submit(() -> {
                    log.info("{} started", id);
                    //每个任务耗时10秒
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                    }
                    log.info("{} finished", id);
                });
            } catch (Exception ex) {
                //提交出现异常的话，打印出错信息并为计数器减一
                log.error("error submitting task {}", id, ex);
                atomicInteger.decrementAndGet();
            }
        });

        TimeUnit.SECONDS.sleep(60);
        return atomicInteger.intValue();
    }

    /**
     * ===========线程池未复用导致的异常==============
     * @return
     * @throws InterruptedException
     */
    @GetMapping("wrong")
    public String wrong() throws InterruptedException {
        ThreadPoolExecutor threadPool = ThreadPoolHelper.getThreadPool();
        IntStream.rangeClosed(1, 10).forEach(i -> {
            threadPool.execute(() -> {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                }
            });
        });
        return "OK";
    }

    /**
     * ==============线程池混用导致的异常====================
     */
    private static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            2, 2,
            1, TimeUnit.HOURS,
            new ArrayBlockingQueue<>(100),
            new ThreadFactoryBuilder().withNameFormat("batchfileprocess-threadpool-%d").get(),
            new ThreadPoolExecutor.CallerRunsPolicy());


    // @PostConstruct
    // public void init() {
    //     printStats(threadPool);
    //
    //     new Thread(() -> {
    //         //模拟需要写入的大量数据
    //         String payload = IntStream.rangeClosed(1, 1_000_000)
    //                 .mapToObj(__ -> "a")
    //                 .collect(Collectors.joining(""));
    //         while (true) {
    //             threadPool.execute(() -> {
    //                 try {
    //                     //每次都是创建并写入相同的数据到相同的文件
    //                     Files.write(Paths.get("demo.txt"), Collections.singletonList(LocalTime.now().toString() + ":" + payload), UTF_8, CREATE, TRUNCATE_EXISTING);
    //                 } catch (IOException e) {
    //                     e.printStackTrace();
    //                 }
    //                 log.info("batch file processing done");
    //             });
    //         }
    //     }).start();
    // }


    private Callable<Integer> calcTask() {
        return () -> {
            TimeUnit.MILLISECONDS.sleep(10);
            return 1;
        };
    }

    @GetMapping("/threadpoolmixuse/wrong")
    public int wrongFixedThreadPool() throws ExecutionException, InterruptedException {
        return threadPool.submit(calcTask()).get();
    }






    private void printStats(ThreadPoolExecutor threadPool) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            log.info("=========================");
            log.info("Pool Size: {}", threadPool.getPoolSize());
            log.info("Active Threads: {}", threadPool.getActiveCount());
            log.info("Number of Tasks Completed: {}", threadPool.getCompletedTaskCount());
            log.info("Number of Tasks in Queue: {}", threadPool.getQueue().size());

            log.info("=========================");
        }, 0, 1, TimeUnit.SECONDS);
    }
}


class ThreadPoolHelper {
    public static ThreadPoolExecutor getThreadPool() {
        //线程池没有复用
        return (ThreadPoolExecutor) Executors.newCachedThreadPool();
    }
    // 声明静态变量复用线程池
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            10, 50,
            2, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1000),
            new ThreadFactoryBuilder().withNameFormat("demo-threadpool-%d").get());
    public static ThreadPoolExecutor getRightThreadPool() {
        return threadPoolExecutor;
    }

}


