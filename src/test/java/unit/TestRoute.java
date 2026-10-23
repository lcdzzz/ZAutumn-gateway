package unit;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import indi.lcdzzz.ApplicationMain;
import indi.lcdzzz.gateway.utils.Pair;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;

import indi.lcdzzz.gateway.utils.ZAutumnGatewayUtils;

import java.util.ArrayList;
import java.util.List;

@Tag("integration")
@SpringBootTest(classes = ApplicationMain.class, properties = {
        "zautumn.application.name=zautumn-integration", "zautumn.application.group=test",
        "spring.redis.host=${TEST_REDIS_HOST:127.0.0.1}", "spring.redis.port=${TEST_REDIS_PORT:16379}",
        "spring.redis.password=${TEST_REDIS_PASSWORD:}"
})
public class TestRoute {

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    /**
     * 修改路由
     */
    @Test
    public void testSetRoute() {
        // 要添加的路由配置内容，JSON格式
        String routeValue =                 "    {\n" +
                "        \"filters\": [\n" +
                "            {\n" +
                "                \"args\": {\n" +
                "                    \"regexp\": \"/test/(?<segment>.*)\",\n" +
                "                    \"replacement\": \"/${segment}\"\n" +
                "                },\n" +
                "                \"name\": \"RewritePath\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"args\": {\n" +
                "                    \"retries\": \"3\",\n" +
                "                    \"methods\": \"GET\"\n" +
                "                },\n" +
                "                \"name\": \"Retry\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"id\": \"all-in-one-route\",\n" +
                "        \"metadata\": {},\n" +
                "        \"order\": 1,\n" +
                "        \"predicates\": [\n" +
                "            {\n" +
                "                \"args\": {\n" +
                "                    \"pattern\": \"/test/**\"\n" +
                "                },\n" +
                "                \"name\": \"Path\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"args\": {\n" +
                "                    \"methods\": \"GET\"\n" +
                "                },\n" +
                "                \"name\": \"Method\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"uri\": \"http://127.0.0.1:9201\"\n" +
                "    }\n";

        // 尝试转换为路由模型类
        RouteDefinition rd = JSON.parseObject(routeValue, RouteDefinition.class);
        JSONArray array = JSONArray.of(rd);
        String routeId = rd.getId();
        System.out.println(array);
        redisTemplate.opsForHash().put(ZAutumnGatewayUtils.getRouteKey(), routeId, array.toJSONString()).block();
        // 发送路由刷新消息
        refreshVersion();
    }

    @Test
    public void testGetRoute() {
        List<RouteDefinition> block = redisTemplate.opsForHash().entries(ZAutumnGatewayUtils.getRouteKey())
                .flatMap(entry -> {
                    String routeId = (String) entry.getKey();
                    List<RouteDefinition> jsonArray = JSONArray.parseArray((String) entry.getValue(), RouteDefinition.class);
                    List<Pair<String, RouteDefinition>> values = new ArrayList<>(jsonArray.size());
                    for (RouteDefinition route : jsonArray) {
                        values.add(Pair.of(routeId, route));
                    }
                    return Flux.fromIterable(values);
                }).filter(pair -> {
                    if (null == pair.v) {
                        return false;
                    } else if (Strings.isBlank(pair.v.getId())
                            || null == pair.v.getUri()) {
                        return false;
                    } else {
                        return true;
                    }
                }).map(Pair::getValue)
                .collectList().block();
        System.out.println(JSON.toJSONString(block));
    }

    @Test
    public void testDeleteRoute() {
        // 要删除的ID
        String routeID = "TEST";
        redisTemplate.opsForHash().remove(ZAutumnGatewayUtils.getRouteKey(), routeID).block();
        // 发送路由刷新消息
        refreshVersion();
    }

    /**
     * 清除所有路由
     */
    @Test
    public void clearAll() {
        redisTemplate.opsForHash().delete(ZAutumnGatewayUtils.getRouteKey()).block();
        refreshVersion();
    }

    private void refreshVersion() {
        redisTemplate.opsForValue().set(ZAutumnGatewayUtils.getVersionKey(), String.valueOf(System.currentTimeMillis())).block();
        redisTemplate.convertAndSend(ZAutumnGatewayUtils.getChannelKey(), "测试更新信息").block();
    }

}
