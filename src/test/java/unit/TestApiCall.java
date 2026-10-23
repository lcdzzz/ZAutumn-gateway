package unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import indi.lcdzzz.ApplicationMain;
import indi.lcdzzz.gateway.apicall.LocalGatewayInvoker;
import indi.lcdzzz.gateway.ZAutumnGatewayChangeGuarantor;

@Tag("integration")
@SpringBootTest(classes = ApplicationMain.class, properties = {
        "zautumn.application.name=zautumn-integration", "zautumn.application.group=test",
        "spring.redis.host=${TEST_REDIS_HOST:127.0.0.1}", "spring.redis.port=${TEST_REDIS_PORT:16379}",
        "spring.redis.password=${TEST_REDIS_PASSWORD:}"
})
public class TestApiCall {

    @Autowired
    private LocalGatewayInvoker gatewayInvoker;

    @Autowired
    private ZAutumnGatewayChangeGuarantor ZAutumnGatewayChangeGuarantor;

    @Test
    public void test() {
        ZAutumnGatewayChangeGuarantor.updateAll().block();
        Mono<String> stringMono = gatewayInvoker.invokeThroughGateway("api", "/testFlow", "{}");
        System.out.println(stringMono.block());
    }
}
