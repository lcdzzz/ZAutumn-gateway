package unit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import indi.lcdzzz.ApplicationMain;
import indi.lcdzzz.gateway.apicall.LocalGatewayInvoker;
import indi.lcdzzz.gateway.ZAutumnGatewayChangeGuarantor;

@SpringBootTest(classes = ApplicationMain.class)
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
