package com.ewancle.service;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ScheduledService {

    /*@Scheduled(
            every = "10s",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP
    )
    void runSingleRunningJob() throws InterruptedException {

        System.out.println("开始执行：" + System.currentTimeMillis());

        // 模拟耗时任务
        Thread.sleep(15000);

        System.out.println("执行结束：" + System.currentTimeMillis());
    }*/

    @Scheduled(
            every = "10s",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP
    )
    public Uni<Void> runSingleRunningJobAsync() {
        System.out.println("开始执行");
        return Uni.createFrom().voidItem()
                .onItem().delayIt().by(java.time.Duration.ofSeconds(15))
                .invoke(() -> System.out.println("执行完成"));
    }

    @Scheduled(
            // | 需求      | 表达式             |
            //| ------- | --------------- |
            //| 每5分钟    | `0 0/5 * * * ?` |
            //| 每小时     | `0 0 * * * ?`   |
            //| 每天凌晨1点  | `0 0 1 * * ?`   |
            //| 每周一凌晨2点 | `0 0 2 ? * MON` |
            //| 每月1号    | `0 0 0 1 * ?`   |
            cron = "0 0/5 * * * ?", // 每5分钟
            delay = 30, // 延迟执行
            delayUnit= TimeUnit.SECONDS,
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP
    )
    public Uni<Void> run() {

        return Uni.createFrom().voidItem()
                .invoke(() -> System.out.println("开始执行任务"))
                .call(this::doBusinessLogic)
                .onFailure().invoke(this::logError);
    }
    private Uni<Void> doBusinessLogic() {
        return Uni.createFrom().voidItem();
    }
    private void logError(Throwable e) {
        CompletableFuture.runAsync(e::printStackTrace);
    }

        // 每 10 秒执行一次 every 支持固定间隔，单位：s 秒，m 分钟，h 小时
    /*@Scheduled(every="10s")
    void everyTenSeconds() {
        System.out.println("执行任务：" + System.currentTimeMillis());
    }*/

    // 每天凌晨 1 点执行 cron 支持标准 cron 表达式
    /*@Scheduled(cron="0 0 1 * * ?")
    void dailyTask() {
        System.out.println("每天凌晨执行任务");
    }*/

    // 如果你的任务耗时较长，建议返回 Uni<Void> 避免阻塞：
    /*@Scheduled(every="5s")
    Uni<Void> runAsync() {
        return Uni.createFrom().item(() -> {
            System.out.println("异步任务执行：" + Thread.currentThread().getName());
            return null;
        });
    }*/

    /*@Scheduled(every="30s")
    void conditionalTask() {
        *//*if(!"prod".equals(ProfileManager.getActiveProfile())) {
            return; // 仅非生产环境执行
        }*//*
        System.out.println("生产环境执行任务");
    }*/

    // 延迟任务（只执行一次） delay：延迟执行, identity：任务唯一标识，可用于取消/管理
    /*@Scheduled(delay=10, delayUnit= TimeUnit.SECONDS,identity="oneTimeTask")
    void runOnce() {
        System.out.println("延迟 10 秒执行一次任务");
    }*/

    // 控制任务并发: 默认情况下，定时任务是串行的。如果希望并行执行：
    // SKIP → 上一次没完成则跳过，如果前一次没完成，跳过本次
    /*@Scheduled(every="5s", concurrentExecution=Scheduled.ConcurrentExecution.SKIP)
    void skipIfRunning() {
        System.out.println("任务正在运行，跳过本次");
    }*/
    // 支持重试
    /*@Scheduled(every = "10s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public Uni<Void> heavyTask() {
        //return someAsyncCall().onFailure().retry().atMost(3);
    }*/

    /*@Scheduled(every="5s", concurrentExecution=Scheduled.ConcurrentExecution.PROCEED)
    // 允许并发执行 不管前一次是否执行完，继续执行
    void allowConcurrent() {
        System.out.println("允许并发执行任务");
    }*/
}
