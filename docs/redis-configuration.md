# Redis 配置协议

以下以默认应用标识 `gateway-api-server@lcdzzz` 为例。键名由 `ZAutumnGatewayUtils` 定义；应用名和分组分别来自 `zautumn.application.name`、`zautumn.application.group`。

## 当前读取的键

| 键 | Redis 类型 | 内容 |
| --- | --- | --- |
| `ZAUTUMN_GATEWAY_ROUTE:gateway-api-server@lcdzzz` | Hash | field 为配置资源 ID，value 为路由对象的 JSON **数组** |
| `ZAUTUMN_GATEWAY_LIMIT_ROUTE:gateway-api-server@lcdzzz` | Hash | field 为限流规则资源 ID，value 为 `GatewayFlowRule` JSON **数组** |
| `ZAUTUMN_GATEWAY_CONFIG_VERSION:gateway-api-server@lcdzzz` | String | 可解析为 Java Long 的递增整数版本号 |
| `ZAUTUMN_GATEWAY_UPDATE_CHANNEL:gateway-api-server@lcdzzz` | Pub/Sub 通道 | 任意文本通知；消息正文不参与解析 |

`ZAUTUMN_GATEWAY_LIMIT_API_GROUP:` 和 `ZAUTUMN_GATEWAY_LIMIT_API_DEFINITION:` 只有预留常量，没有加载 API 分组规则的实现。旧 README 的 `ZAUTUMN_GATEWAY_LIMIT:` 不是当前限流数据源读取的键。

## 路由数据

见 [examples/route.json](../examples/route.json)：

```json
[
  {
    "id": "demo-route",
    "uri": "http://127.0.0.1:18080",
    "predicates": [
      {"name": "Path", "args": {"pattern": "/demo/**"}},
      {"name": "Method", "args": {"methods": "GET"}}
    ],
    "filters": [{"name": "StripPrefix", "args": {"parts": "1"}}],
    "order": 0
  }
]
```

Hash 的一个 field 可以保存多条路由。每条路由应有全局唯一的 `id` 和有效 `uri`。Gateway 的 `args` 使用字符串值。配置 Predicate、Filter 时请核对当前 Gateway 版本实际提供的工厂；不要假定任意名称都能使用。

## 路由限流

见 [examples/limit-rule.json](../examples/limit-rule.json)：`resource` 必须匹配路由 `id`，`resourceMode: 0` 表示路由 ID，`grade: 1` 表示 QPS，`count: 10`、`intervalSec: 1` 表示每秒 10 次。

```json
[
  {
    "resource": "demo-route",
    "resourceMode": 0,
    "grade": 1,
    "count": 10,
    "intervalSec": 1,
    "controlBehavior": 0
  }
]
```

## 发布顺序

1. 先用 `HSET` 写入完整路由和限流数组，删除路由用 `HDEL` 删除对应 field。
2. `SET` 一个大于上次的版本号。可使用递增计数器或单调递增时间戳；同一秒多次发布不能复用同一个秒级时间戳。
3. `PUBLISH` 通知立即刷新。不要只依赖消息送达。

启动时会尝试加载路由和限流规则；运行期间监听通道，每 10 秒检查一次版本。轮询只在版本增大、且距最近通知超过 15 秒时刷新。消息触发后通过 `RefreshRoutesEvent` 通知 Gateway 重建路由。

## 已知限制

- 路由和限流读取不构成原子快照；配置发布需要调用方控制写入顺序与一致性。双通道触发不等于可靠重试或事务保证。
- 非法 JSON 可导致刷新失败；当前尚无完整的配置校验、审计和回滚能力。
- `RedisSentinelRouteDataSource.update()` 仅在加载到非空规则时更新 Sentinel。因此删除全部限流规则不会清空进程内旧规则；需要重启网关，或先实现并验证规则清空逻辑。
- `RedisRouteDefinitionRepository.save()` 写入单对象，而读取路径按数组解析。目前演示及配置发布应直接写入数组；不要通过该方法发布配置，直到读写格式统一。
- 应用标识在静态键初始化时读取，依赖 Spring 组件初始化次序。更改标识后应重启并核对实际读取的键。
- Redis 不可用时，HTTP 服务启动成功不代表配置已经加载；请检查路由加载日志并做真实转发验证。
- 尚无配置管理接口或权限控制。Redis 写权限相当于控制网关转发目标，应限制网络访问和账户权限。
