package indi.lcdzzz.gateway.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
@Component
@ConditionalOnProperty(name = "zautumn.gateway.log-filter.enabled", havingValue = "true", matchIfMissing = false)
public class GatewayLogFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(GatewayLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String originalPath = request.getURI().getPath();

        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route != null) {
            String targetUri = route.getUri().toString();

            // 记录原始信息到exchange属性中
            exchange.getAttributes().put("originalPath", originalPath);
            exchange.getAttributes().put("routeId", route.getId());
            exchange.getAttributes().put("targetUri", targetUri);

            // 继续执行过滤器链，并在完成后记录日志
            return chain.filter(exchange)
                    .doFinally(signalType -> logForwardInfo(exchange, route));
        }

        return chain.filter(exchange);
    }

    private void logForwardInfo(ServerWebExchange exchange, Route route) {
        String originalPath = exchange.getAttribute("originalPath");
        String targetUri = exchange.getAttribute("targetUri");

        if (originalPath != null && targetUri != null) {
            // 获取经过RouteToRequestUrlFilter处理后的最终URL
            URI finalUrl = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            String finalTarget = finalUrl != null ? finalUrl.toString() : targetUri + originalPath;

            logger.info("请求路径: {}, 路由ID: {}, 转发到: {}",
                    originalPath, route.getId(), finalTarget);
        }
    }

    @Override
    public int getOrder() {
        return -1; // 改为较低的优先级值，避免干扰其他过滤器
    }
}

