package indi.lcdzzz.gateway.apicall;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class LocalGatewayInvoker {
    private static final MediaType APPLICATION_JSON = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

    private final HandlerMapping handlerMapping;

    @Value("http://127.0.0.1:${server.port}/")
    private String localPath;

    public LocalGatewayInvoker(@Qualifier("routePredicateHandlerMapping") HandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    public Mono<String> invokeThroughGateway(String serviceName, String path, String body) {
        return invokeThroughGateway(serviceName, path, body, HttpMethod.POST);
    }

    public Mono<String> invokeThroughGateway(String serviceName, String path, String body, HttpMethod method) {
        // 1. 创建ServerHttpRequest
        ZAutumnServerHttpRequestMask request = ZAutumnServerHttpRequestMask.method(method, localPath + serviceName + path)
                .contentType(APPLICATION_JSON)
                .body(body);

        // 2. 创建ServerWebExchange
        ServerWebExchange exchange = ZAutumnServerWebExchange.from(request);

        // 3.查找合适的WEB处理器
        Object handler = handlerMapping.getHandler(exchange).block();
        if (handler instanceof WebHandler) {
            // 4. 处理请求
            return ((WebHandler) handler).handle(exchange).then(Mono.fromSupplier(() -> {
                // 5. 获取响应体
                ZAutumnServerHttpResponseMask response = (ZAutumnServerHttpResponseMask) exchange.getResponse();
                return response.getBodyAsString();
            })).block();
        }

        // 未找到合适的处理器
        return Mono.empty();

    }
}
