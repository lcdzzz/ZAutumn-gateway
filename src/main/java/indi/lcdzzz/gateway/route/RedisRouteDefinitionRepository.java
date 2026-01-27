package indi.lcdzzz.gateway.route;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import indi.lcdzzz.gateway.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import indi.lcdzzz.gateway.ZAutumnGatewayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;


/**
 * 路由配置存储与加载
 */
@Component
public class RedisRouteDefinitionRepository implements RouteDefinitionRepository {
    private static final Logger logger = LoggerFactory.getLogger(RedisRouteDefinitionRepository.class);

    private final ReentrantLock lock = new ReentrantLock();


    private final ReactiveStringRedisTemplate redisTemplate;

    private List<Pair<String, RouteDefinition>> cachedRoutes = new ArrayList<>();

    public RedisRouteDefinitionRepository(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 获取所有路由（缓存）
    public Flux<RouteDefinition> getRouteDefinitions() {
        return Flux.fromIterable(cachedRoutes).map(Pair::getValue);
    }

    // 保存路由定义
    public Mono<Void> save(Mono<RouteDefinition> routeDefinitionMono) {
        return routeDefinitionMono.flatMap(route -> {
            String json = JSON.toJSONString(route);
            return redisTemplate.opsForHash().put(ZAutumnGatewayUtils.getRouteKey(), route.getId(), json).then();
        });
    }

    // 删除路由
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.flatMap(id -> redisTemplate.opsForHash().remove(ZAutumnGatewayUtils.getRouteKey(), id).then());
    }

    // 刷新路由（对外提供手动刷新接口）   redisTemplate.opsForHash().entries(zautumnGatewayUtils.getRouteKey()).collectList().block()
    public Mono<Void> refreshRoutes() {
        return redisTemplate.opsForHash().entries(ZAutumnGatewayUtils.getRouteKey()).flatMap(entry -> {
            String routeId = (String) entry.getKey();
            List<RouteDefinition> jsonArray = JSONArray.parseArray((String) entry.getValue(), RouteDefinition.class);
            List<Pair<String, RouteDefinition>> values = new ArrayList<>(jsonArray.size());
            for (RouteDefinition route : jsonArray) {
                values.add(Pair.of(routeId, route));
            }
            return Flux.fromIterable(values);
        }).filter(new Predicate<Pair<String, RouteDefinition>>() {
            @Override
            public boolean test(Pair<String, RouteDefinition> pair) {
                if (null == pair.v) {
                    logger.warn("跳过空的路由配置，请检查缓存中是否存在废弃数据！");
                    return false;
                } else if (StringUtils.isBlank(pair.v.getId()) || null == pair.v.getUri()) {
                    logger.warn("跳过错误的路由配置，请检查缓存中是否存在废弃数据，路由ID：{}，内容：{}", pair.k, pair.v);
                    return false;
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("检测到路由规则，路由ID：{}，内容：{}", pair.k, pair.v);
                    }
                    return true;
                }
            }
        }).collectList().doOnNext(routes -> {
            lock.lock();
            try {
                cachedRoutes = routes;
                logger.info("路由缓存已刷新，共加载 {} 条路由", cachedRoutes.size());
            } finally {
                lock.unlock();
            }

        }).then();
    }

}