package indi.lcdzzz.gateway.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 动态路由定义定位器
 */
@Component("dynamicRouteDefinitionLocator")
public class DynamicRouteDefinitionLocator implements RouteDefinitionLocator {
    private static final Logger logger = LoggerFactory.getLogger(DynamicRouteDefinitionLocator.class);

    private final RedisRouteDefinitionRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public DynamicRouteDefinitionLocator(RedisRouteDefinitionRepository repository,ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return repository.getRouteDefinitions();  // 返回从Redis获取的路由定义
    }

    // 外部可调用的缓存刷新方法，并推送路由更新消息
    public Mono<Void> refreshCache() {
        return repository.refreshRoutes().then(Mono.fromRunnable(() ->
                //这个事件会被 Spring Cloud Gateway 内部的路由刷新机制接收，从而更新实际的路由配置
                eventPublisher.publishEvent(new RefreshRoutesEvent(this))));
    }

    /**
     * eventPublisher.publishEvent(new RefreshRoutesEvent(this))); 的作用
     * 1. 触发路由刷新事件
     *      向 Spring 应用上下文发布一个 RefreshRoutesEvent 事件
     *      通知系统中监听此事件的组件进行路由配置更新
     * 2. Spring Cloud Gateway 内部处理
     *      Spring Cloud Gateway 内置的路由刷新机制会监听此类事件
     *      接收到事件后，会重新加载路由配置并更新运行时路由表
     * 3. 实现动态路由更新
     *      使网关能够在运行时动态更新路由规则
     *      无需重启服务即可生效新的路由配置
     * 4. 事件驱动架构
     *      采用观察者模式，解耦路由配置变更与路由刷新逻辑【重点】
     *      确保路由更新过程的可靠性和一致性
     * 5. 配合缓存刷新
     *      在 repository.refreshRoutes() 完成数据层更新后
     *      通过事件机制触发框架层的路由表重建
     */
}