package indi.lcdzzz.gateway.sentinel;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.datasource.AbstractDataSource;
import com.alibaba.fastjson.JSONArray;
import indi.lcdzzz.gateway.ZAutumnGatewayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 限流数据源
 */
@Component
public class RedisSentinelRouteDataSource extends AbstractDataSource<Flux<GatewayFlowRule>, Set<GatewayFlowRule>> {
    private static final Logger logger = LoggerFactory.getLogger(RedisSentinelRouteDataSource.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private ScheduledExecutorService service;

    public RedisSentinelRouteDataSource(ReactiveStringRedisTemplate redisTemplate) {
        super(flux -> new HashSet<>(flux.collectList().block()));
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Flux<GatewayFlowRule> readSource() {
        // 读取限流信息
        return redisTemplate.opsForHash().values(ZAutumnGatewayUtils.getLimitRouteKey()).flatMapIterable(json -> JSONArray.parseArray((String) json, GatewayFlowRule.class));
    }

    @Override
    public void close() throws Exception {
        if (service != null) {
            service.shutdownNow();
            service = null;
        }
    }

    public Mono<Void> update() {
        try {
            Flux<GatewayFlowRule> gatewayFlowRuleFlux = readSource();
            return gatewayFlowRuleFlux.collectList().doOnNext(newValue -> {
                if (null != newValue && !newValue.isEmpty()) {
                    logger.info("加载路由限流规则[{}]条...", newValue.size());
                    getProperty().updateValue(new HashSet<>(newValue));
                } else {
                    logger.info("未加载到限流规则...");
                }
            }).then();
        } catch (Throwable e) {
            logger.error("加载路由限流规则发生异常！", e);
            return Mono.empty();
        }
    }
}