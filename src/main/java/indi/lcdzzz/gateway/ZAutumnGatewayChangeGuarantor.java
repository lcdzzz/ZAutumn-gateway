package indi.lcdzzz.gateway;

import indi.lcdzzz.gateway.route.DynamicRouteDefinitionLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


/**
 * 星云网关动态更新确保模块
 */
@Component
public class ZAutumnGatewayChangeGuarantor implements ApplicationListener<ApplicationStartedEvent>, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(ZAutumnGatewayChangeGuarantor.class);

    private final DynamicRouteDefinitionLocator routeLocator;
    private final ReactiveStringRedisTemplate redisTemplate;
    // 使用双时间戳机制
    private volatile long lastCheckVersion = -1;
    private volatile long lastChangeTimestamp = -1;

    private ScheduledExecutorService scheduler;

    public ZAutumnGatewayChangeGuarantor(DynamicRouteDefinitionLocator routeLocator, ReactiveStringRedisTemplate redisTemplate) {
        this.routeLocator = routeLocator;
        this.redisTemplate = redisTemplate;
    }

    public void init() {
        // 初始化版本号
        getCurrentVersion().subscribe(version -> {
            lastCheckVersion = version;
            logger.info("初始网关配置信息版本号: {}", version);
        });
        // 注册Sentinel限流规则变更监听器
        // 立即刷新路由和限流信息
        updateAll().subscribe();
        // 双重监控机制
        startPollingTask();
        startPubSubListener();
    }

    /**
     * 获取当前路由版本号，如果为0说明没有路由信息。
     */
    public Mono<Long> getCurrentVersion() {
        return redisTemplate.opsForValue().get(ZAutumnGatewayUtils.getVersionKey())
                .flatMap(val -> val != null ? Mono.just(Long.parseLong(val)) : Mono.just(0L))
                .onErrorReturn(0L);
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        init();
    }

    // 1. 定时轮询任务
    private void startPollingTask() {
        scheduler = Executors.newSingleThreadScheduledExecutor();// 创建单线程的定时任务调度器
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkVersionChange().block(Duration.ofSeconds(5));
            } catch (Exception e) {
                logger.error("轮询检查异常", e);
            }
        }, 10, 10, TimeUnit.SECONDS); // 10秒一次
        // 第一个10是首次延迟10秒后开始执行，第二个10是执行间隔时间
    }

    // 2. Pub/Sub监听器
    private void startPubSubListener() {
        Disposable subscribe = redisTemplate.listenToChannel(ZAutumnGatewayUtils.getChannelKey())
                .doOnNext(message -> {
                    logger.info("收到实时变更通知: {}", message);
                    getCurrentVersion().subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long currentVersion) {
                            // 更新版本号
                            logger.info("网关配置版本变更 ({}->{})", lastCheckVersion, currentVersion);
                            lastCheckVersion = currentVersion;
                            lastChangeTimestamp = System.currentTimeMillis();

                            try {
                                // 立即刷新路由信息和限流信息
                                ZAutumnGatewayChangeGuarantor.this.updateAll().subscribe();
                            } catch (Exception e) {
                                logger.error("刷新路由信息发生异常！", e);
                            }

                        }
                    });
                })
                .subscribe();
    }

    // 检测Redis中网关配置版本是否变更
    private Mono<Void> checkVersionChange() {
        return getCurrentVersion().flatMap(currentVersion -> {
            if (currentVersion > lastCheckVersion) {
                // 如果最近没有收到实时通知，强制刷新
                long now = System.currentTimeMillis();
                if (now - lastChangeTimestamp > 15000) {
                    logger.info("未收到实时通知，主动检测到网关配置版本变更 ({}->{})，强制刷新网关配置", lastCheckVersion, currentVersion);
                    lastCheckVersion = currentVersion;
                    return updateAll();
                }
            }
            return Mono.empty();
        });
    }

    /**
     * 更新路由信息和限流配置信息
     */
    public Mono<Void> updateAll() {
        // 任务1 刷新路由信息
        Mono<Void> task1 = routeLocator.refreshCache();
        // 任务2 刷新限流信息
        return task1;
    }

    @Override
    public void destroy() {
        if (null != scheduler) {
            try {
                scheduler.shutdown();
            } catch (Exception e) {
                scheduler.shutdownNow();
            }
        }
    }
}