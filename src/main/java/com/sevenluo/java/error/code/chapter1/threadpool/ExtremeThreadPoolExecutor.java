package com.sevenluo.java.error.code.chapter1.threadpool;

import java.util.concurrent.*;

/**
 * @author: sevenluo
 * @date: 2022/04/19 07:45
 * @description: 自定义激进线程池，功能：当没有达到最大线程数时，先创建线程到最大线程数，然后再加入队列中排队
 * 实现思路：
 * 1. 重写LinkedBlockingQueue的 offer 方法，使其返回 false；
 * 2. 然后再线程池的RejectedExecutionHandler策略处理逻辑中将任务加入队列排队
 */
public class ExtremeThreadPoolExecutor {

    public ExtremeThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,int blockQueueSize) {
        // 扩展LinkedBlockingQueue，强制offer()有条件地返回false
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(blockQueueSize) {
            private static final long serialVersionUID = -6903933921423432194L;
            @Override
            public boolean offer(Runnable e) {
                // Offer it to the queue if there is 0 items already queued, else
                // return false so the TPE will add another thread. If we return false
                // and max threads have been reached then the RejectedExecutionHandler
                // will be called which will do the put into the queue.
                if (size() == 0) {
                    return super.offer(e);
                } else {
                    return false;
                }
            }
        };
        RejectedExecutionHandler rejectedExecutionHandler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                try {
                    //这是实际放入队列的地方，如果最大线程数满了则放入队列，队列满了则排队等待
                    executor.getQueue().put(r);
                    // 也可以使用 offer 方法，如果队列满了则直接丢弃，而不是排队
                    // boolean offer = executor.getQueue().offer(r, 10, TimeUnit.MILLISECONDS);
                    // if (!offer) {
                    //     throw new RejectedExecutionException("Task " + r +
                    //             " rejected from " + executor);
                    // }
                    // 如果线程池已经关闭，则抛出异常
                    if (executor.isShutdown()) {
                        throw new RejectedExecutionException(
                                "Task " + r + " rejected from " + executor);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        };
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1 /*core*/, 50 /*max*/,
                60 /*secs*/, TimeUnit.SECONDS, queue,Executors.defaultThreadFactory(),rejectedExecutionHandler);

    }


}
