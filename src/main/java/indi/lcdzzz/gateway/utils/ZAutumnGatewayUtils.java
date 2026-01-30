package indi.lcdzzz.gateway.utils;


public class ZAutumnGatewayUtils {
    /**
     * 星云网关配置信息更新渠道KEY 前缀
     */
    public static final String ROUTE_CHANNEL_PREFIX = "ZAUTUMN_GATEWAY_UPDATE_CHANNEL:";

    /**
     * 星云网关配置信息版本KEY 前缀
     */
    public static final String ROUTE_VERSION_PREFIX = "ZAUTUMN_GATEWAY_CONFIG_VERSION:";

    /**
     * 星云网关路由：路由配置信息KEY 前缀
     */
    public static final String ROUTE_KEY_PREFIX = "ZAUTUMN_GATEWAY_ROUTE:";

    /**
     * 星云网关限流：路由限流配置信息KEY 前缀
     */
    public static final String LIMIT_ROUTE_PREFIX = "ZAUTUMN_GATEWAY_LIMIT_ROUTE:";

    /**
     * 星云网关限流：API分组限流配置信息KEY 前缀
     */
    public static final String LIMIT_API_PREFIX = "ZAUTUMN_GATEWAY_LIMIT_API_GROUP:";

    /**
     * 星云网关限流：API分组定义配置信息KEY 前缀
     */
    public static final String LIMIT_API_DEFINITION_PREFIX = "ZAUTUMN_GATEWAY_LIMIT_API_DEFINITION:";

    private static final String routeKey;
    private static final String channelKey;
    private static final String versionKey;
    private static final String limitRouteKey;
    private static final String limitApiKey;
    private static final String limitApiDefinitionKey;

    static {
        channelKey = ROUTE_CHANNEL_PREFIX + Applications.getName() + '@' + Applications.getGroup();
        versionKey = ROUTE_VERSION_PREFIX + Applications.getName() + '@' + Applications.getGroup();
        routeKey = ROUTE_KEY_PREFIX + Applications.getName() + '@' + Applications.getGroup();
        limitRouteKey = LIMIT_ROUTE_PREFIX + Applications.getName() + '@' + Applications.getGroup();
        limitApiKey = LIMIT_API_PREFIX + Applications.getName() + '@' + Applications.getGroup();
        limitApiDefinitionKey = LIMIT_API_DEFINITION_PREFIX + Applications.getName() + '@' + Applications.getGroup();
    }

    public static String getChannelKey() {
        return channelKey;
    }

    public static String getLimitRouteKey() {
        return limitRouteKey;
    }

    public static String getLimitApiKey() {
        return limitApiKey;
    }

    public static String getLimitApiDefinitionKey() {
        return limitApiDefinitionKey;
    }

    public static String getVersionKey() {
        return versionKey;
    }

    public static String getRouteKey() {
        return routeKey;
    }
}
