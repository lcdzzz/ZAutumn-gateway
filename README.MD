# ZAutumn网关

音：[/zɛd ˈɔːtəm/]

Z秋天

### 实现方案
##### 基于SpringCloud Gateway和Redis动态刷新路由
网关会轮询REDIS中的版本号并实时监听消息通道，无论通过版本还是消息通道得到刷新指令，都会刷新路由、熔断配置，管理端可以先更新路由、熔断信息，然后更新路由版本同时往通道发送消息，由版本号和通道消息对网关路由刷新进行双重保障。

- Redis监听配置版本的KEY值为 `ZAUTUMN_GATEWAY_CONFIG_VERSION:应用名@分组名`
- Redis监听配置变化的消息通道为 `ZAUTUMN_GATEWAY_UPDATE_CHANNEL:应用名@分组名`
- Redis监听路由信息KEY值为 `ZAUTUMN_GATEWAY_ROUTE:应用名@分组名`
- Redis监听限流信息KEY值为 `ZAUTUMN_GATEWAY_LIMIT:应用名@分组名`

### REDIS路由变更测试用例
- 通过set来设置版本号
```
# 若应用名为gateway，分组为group，更新的配置版本号为20250702时，在redis-cli中执行以下命令
SET ZAUTUMN_GATEWAY_CONFIG_VERSION:gateway@group 20250702
```
- 通过PUBLISH来推送更新消息
```
# 若应用名为gateway，分组为group，更新的配置版本号为20250702时，在redis-cli中执行以下命令
PUBLISH ZAUTUMN_GATEWAY_UPDATE_CHANNEL:gateway@group "更新x条路由信息"
```

- 通过hset来设置路由配置信息
```
# 若应用名为gateway，分组为group，更新的配置版本号为20250702时，路由资源ID为all-in-one-route时，在redis-cli中执行以下命令
HSET ZAUTUMN_GATEWAY_ROUTE:gateway@group 'all-in-one-route' '[{"id":"all-in-one-route","uri":"https://www.sojson.com/yasuoyihang.html","predicates":[{"name":"Path","args":{"pattern":"/api/**"}},{"name":"Method","args":{"methods":"POST"}}],"filters":[{"name":"StripPrefix","args":{"parts":1}},{"name":"RewritePath","args":{"regexp":"/api/(?<segment>.*)","replacement":"/v2/${segment}"}},{"name":"Retry","args":{"retries":3,"methods":"POST"}}],"order":1}]'
```json
{
  "id": "all-in-one-route",
  "uri": "https://www.sojson.com/yasuoyihang.html",
  "predicates": [
    {"name": "Path", "args": {"pattern": "/api/**"}},
    {"name": "Method", "args": {"methods": "POST"}}
  ],
  "filters": [
    {"name": "StripPrefix", "args": {"parts": 1}},
    {"name": "RewritePath", "args": {"regexp": "/api/(?<segment>.*)", "replacement": "/flow/${segment}"}},
    {"name": "Retry", "args": {"retries": 3, "methods": "POST"}},
    {
      "name": "Cors",
      "args": {
        "corsConfigurations": {
          "[{\"path\": \"/api/users/**\"}]": {
            "allowedOrigins": ["https://user-app.com"],
            "allowedMethods": ["GET", "POST"],
            "allowedHeaders": ["Content-Type", "Authorization"],
            "exposedHeaders": ["X-User-ID"],
            "maxAge": 3600,
            "allowCredentials": true
          }
        }
      }
    }
  ],
  "order": 1
}
```
### JAVA代码路由变更测试方法
具体查看单元测试中TestRoute类

