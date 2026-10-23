package indi.lcdzzz.gateway.route;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.fastjson2.JSON;
import indi.lcdzzz.gateway.utils.Applications;
import indi.lcdzzz.gateway.utils.ZAutumnGatewayUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RouteConfigurationTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private ReactiveHashOperations<String, Object, Object> hashes;
    private RedisRouteDefinitionRepository repository;

    @BeforeAll
    static void initializeApplicationIdentity() {
        Applications applications = new Applications();
        ReflectionTestUtils.setField(applications, "instanceName", "offline-test");
        ReflectionTestUtils.setField(applications, "instanceGroup", "test");
        applications.init();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        hashes = mock(ReactiveHashOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        repository = new RedisRouteDefinitionRepository(redis);
    }

    @Test
    void exampleRouteAndLimitRuleReferToTheSameResource() throws Exception {
        List<RouteDefinition> routes = JSON.parseArray(readExample("route.json"), RouteDefinition.class);
        List<GatewayFlowRule> limits = JSON.parseArray(readExample("limit-rule.json"), GatewayFlowRule.class);
        assertEquals(1, routes.size());
        RouteDefinition route = routes.get(0);
        assertEquals("demo-route", route.getId());
        assertEquals("http://127.0.0.1:18080", route.getUri().toString());
        assertEquals("/demo/**", route.getPredicates().get(0).getArgs().get("pattern"));
        assertEquals("StripPrefix", route.getFilters().get(0).getName());
        assertEquals("1", route.getFilters().get(0).getArgs().get("parts"));
        assertEquals(1, limits.size());
        assertEquals(route.getId(), limits.get(0).getResource());
        assertEquals(10.0, limits.get(0).getCount());
        assertEquals(1L, limits.get(0).getIntervalSec());
    }

    @Test
    void refreshLoadsEveryRouteInAHashValue() {
        stubEntries("[{\"id\":\"one\",\"uri\":\"http://localhost:18080\"},"
                + "{\"id\":\"two\",\"uri\":\"http://localhost:18081\"}]");
        repository.refreshRoutes().block(TIMEOUT);
        List<RouteDefinition> routes = repository.getRouteDefinitions().collectList().block(TIMEOUT);
        assertNotNull(routes);
        assertEquals(2, routes.size());
        assertEquals("one", routes.get(0).getId());
        assertEquals("two", routes.get(1).getId());
        verify(hashes).entries(ZAutumnGatewayUtils.getRouteKey());
    }

    @Test
    void refreshSkipsNullAndMissingRequiredFields() {
        stubEntries("[null,{\"id\":\"\",\"uri\":\"http://localhost\"},"
                + "{\"id\":\"missing-uri\"},{\"id\":\"valid\",\"uri\":\"http://localhost\"}]");
        repository.refreshRoutes().block(TIMEOUT);
        List<RouteDefinition> routes = repository.getRouteDefinitions().collectList().block(TIMEOUT);
        assertNotNull(routes);
        assertEquals(1, routes.size());
        assertEquals("valid", routes.get(0).getId());
    }

    @Test
    void malformedJsonFailsRefreshAndPreservesPreviousCache() throws Exception {
        stubEntries(readExample("route.json"));
        repository.refreshRoutes().block(TIMEOUT);
        stubEntries("not-json");
        assertThrows(RuntimeException.class, () -> repository.refreshRoutes().block(TIMEOUT));
        assertEquals("demo-route", repository.getRouteDefinitions().blockFirst(TIMEOUT).getId());
    }

    @Test
    void removingAllHashEntriesClearsTheRouteCache() throws Exception {
        stubEntries(readExample("route.json"));
        repository.refreshRoutes().block(TIMEOUT);
        when(hashes.entries(anyString())).thenReturn(Flux.empty());
        repository.refreshRoutes().block(TIMEOUT);
        assertEquals(0L, repository.getRouteDefinitions().count().block(TIMEOUT));
    }

    @Test
    void successfulRefreshPublishesGatewayRefreshEvent() throws Exception {
        stubEntries(readExample("route.json"));
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        DynamicRouteDefinitionLocator locator = new DynamicRouteDefinitionLocator(repository, publisher);
        locator.refreshCache().block(TIMEOUT);
        verify(publisher).publishEvent(isA(RefreshRoutesEvent.class));
        assertEquals("demo-route", locator.getRouteDefinitions().blockFirst(TIMEOUT).getId());
    }

    @Test
    void redisFailureDoesNotPublishASuccessfulRefreshEvent() {
        when(hashes.entries(anyString())).thenReturn(Flux.error(new IllegalStateException("Redis unavailable")));
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        DynamicRouteDefinitionLocator locator = new DynamicRouteDefinitionLocator(repository, publisher);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> locator.refreshCache().block(TIMEOUT));
        assertEquals("Redis unavailable", error.getMessage());
        verifyNoInteractions(publisher);
    }

    private void stubEntries(String json) {
        Map.Entry<Object, Object> entry = new AbstractMap.SimpleEntry<>("resource-id", json);
        when(hashes.entries(anyString())).thenReturn(Flux.just(entry));
    }

    private static String readExample(String name) throws Exception {
        return new String(Files.readAllBytes(Paths.get("examples", name)), StandardCharsets.UTF_8);
    }
}
