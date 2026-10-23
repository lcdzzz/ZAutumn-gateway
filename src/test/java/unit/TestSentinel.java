package unit;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import indi.lcdzzz.ApplicationMain;
import indi.lcdzzz.gateway.utils.ZAutumnGatewayUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Tag("integration")
@SpringBootTest(classes = ApplicationMain.class, properties = {
        "zautumn.application.name=zautumn-integration", "zautumn.application.group=test",
        "spring.redis.host=${TEST_REDIS_HOST:127.0.0.1}", "spring.redis.port=${TEST_REDIS_PORT:16379}",
        "spring.redis.password=${TEST_REDIS_PASSWORD:}"
})

public class TestSentinel {

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Test
    public void testSetLimitRule() {
        // 要添加的限流规则配置内容，JSON格式
        String limitRuleValue =
                "{\n" +
                        "    \"resource\": \"all-in-one-route\",\n" +
                        "    \"resourceMode\": 0,\n" +
                        "    \"grade\": 1,\n" +
                        "    \"count\": 10,\n" +
                        "    \"intervalSec\": 1,\n" +
                        "    \"controlBehavior\": 0,\n" +
                        "    \"burst\": 0,\n" +
                        "    \"maxQueueingTimeoutMs\": 500\n" +
                        "}";

        // 限流规则ID
        String ruleId = "limit-rule-test";
        GatewayFlowRule rule = JSON.parseObject(limitRuleValue, GatewayFlowRule.class);
        JSONArray array = JSONArray.of(rule);
        System.out.println(array);
        // 将限流规则存储到Redis hash中
        redisTemplate.opsForHash().put(ZAutumnGatewayUtils.getLimitRouteKey(), ruleId, array.toJSONString()).block();

        // 发送路由刷新消息，触发限流规则更新
        refreshVersion();
    }


    private void refreshVersion() {
        redisTemplate.opsForValue().set(ZAutumnGatewayUtils.getVersionKey(), String.valueOf(System.currentTimeMillis())).block();
        redisTemplate.convertAndSend(ZAutumnGatewayUtils.getChannelKey(), "测试更新信息").block();
    }


}
