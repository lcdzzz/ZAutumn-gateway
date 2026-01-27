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
                eventPublisher.publishEvent(new RefreshRoutesEvent(this))));
    }
}