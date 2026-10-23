# ZAutumn Gateway

[![CI](https://github.com/lcdzzz/ZAutumn-gateway/actions/workflows/ci.yml/badge.svg)](https://github.com/lcdzzz/ZAutumn-gateway/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 8](https://img.shields.io/badge/Java-8-orange.svg)](pom.xml)

基于 **Spring Cloud Gateway + Redis + Sentinel** 的 Java 网关服务端。读作 /zɛd ˈɔːtəm/，意为「Z 秋天」。

路由和路由级限流规则存放在 Redis 中，通过 Pub/Sub 通知和版本号轮询触发刷新，无需重启网关。适合学习动态网关实现，以及作为自建网关的开发起点。

## 能力与边界

- Redis Hash 保存路由 JSON 数组，支持 Gateway 的 Predicate 和 Filter。
- Redis Pub/Sub 触发刷新；每 10 秒检查一次递增版本号，最近通知后的 15 秒内暂不通过轮询刷新。
- Sentinel 路由级限流规则动态加载。
- 可选请求转发日志，以及 Java 内部调用辅助类 `LocalGatewayInvoker`。

当前不包含管理后台、身份认证、配置发布 API、完整熔断治理或生产部署方案。API 分组相关 Redis 键只是预留常量，目前只有路由级限流数据源接入。现有依赖基线为 Spring Boot 2.7.18、Spring Cloud 2021.0.8、Spring Cloud Alibaba 2021.0.5.0，采用前请评估升级和安全维护成本。

## 快速开始

需要 **JDK 8、Maven 3.8.6+、Redis 6/7**。下面的演示还使用 Docker Compose 和 Python 3；也可以用已有的隔离 Redis 和任意 HTTP 上游。默认 HTTP 端口 `8091`，Redis 地址 `127.0.0.1:6379`，应用标识 `gateway-api-server@lcdzzz`。

```bash
git clone https://github.com/lcdzzz/ZAutumn-gateway.git
cd ZAutumn-gateway
mvn clean verify
```

普通构建只运行不连接 Redis 的自动化测试，并生成可直接运行的 `target/zautumn-gateway-1.0.0.jar`。

### 1. 启动演示依赖

```bash
# 演示 Redis 仅绑定本机 16379，不占用默认 Redis 端口
docker compose up -d redis
# 在单独终端中启动上游，保留此进程
python3 -m http.server 18080 --bind 127.0.0.1 --directory examples/upstream
```

### 2. 启动网关

在另一个终端中运行：

```bash
SPRING_REDIS_PORT=16379 java -jar target/zautumn-gateway-1.0.0.jar
```

看到 `Started ApplicationMain` 后继续。没有配置路由时，访问网关返回 404 属于正常现象。

### 3. 写入路由并通知刷新

在仓库根目录执行，以下命令只针对演示 Redis：

```bash
docker compose exec -T redis redis-cli -x HSET \
  'ZAUTUMN_GATEWAY_ROUTE:gateway-api-server@lcdzzz' demo-route < examples/route.json

docker compose exec -T redis redis-cli SET \
  'ZAUTUMN_GATEWAY_CONFIG_VERSION:gateway-api-server@lcdzzz' "$(date +%s)"

docker compose exec -T redis redis-cli PUBLISH \
  'ZAUTUMN_GATEWAY_UPDATE_CHANNEL:gateway-api-server@lcdzzz' 'demo route updated'

curl --fail http://127.0.0.1:8091/demo/
```

预期返回包含 `ZAutumn Gateway demo upstream` 的 HTML。路由将 `/demo/` 去掉第一段路径后转发到 `http://127.0.0.1:18080/`。发布通知后如果尚未完成刷新，稍候再请求。

可选：加载每秒 10 次的路由限流规则，再更新版本并发送通知：

```bash
docker compose exec -T redis redis-cli -x HSET \
  'ZAUTUMN_GATEWAY_LIMIT_ROUTE:gateway-api-server@lcdzzz' demo-limit < examples/limit-rule.json

docker compose exec -T redis redis-cli SET \
  'ZAUTUMN_GATEWAY_CONFIG_VERSION:gateway-api-server@lcdzzz' "$(date +%s)"
docker compose exec -T redis redis-cli PUBLISH \
  'ZAUTUMN_GATEWAY_UPDATE_CHANNEL:gateway-api-server@lcdzzz' 'demo limit updated'
```

停止演示时，在网关和 Python 终端按 `Ctrl+C`，再运行 `docker compose stop redis`。演示 Redis 没有持久化卷，重建容器会丢失演示数据。

## 配置

Spring Boot 支持通过环境变量覆盖 [application.yml](src/main/resources/application.yml)，无需把真实凭据写入仓库：

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| `SERVER_PORT` | `8091` | HTTP 监听端口 |
| `SPRING_REDIS_HOST` | `127.0.0.1` | Redis 地址 |
| `SPRING_REDIS_PORT` | `6379` | Redis 端口 |
| `SPRING_REDIS_DATABASE` | `0` | Redis 数据库 |
| `SPRING_REDIS_USERNAME` | 未设置 | Redis ACL 用户名（如需） |
| `SPRING_REDIS_PASSWORD` | 未设置 | Redis 密码 |
| `ZAUTUMN_APPLICATION_NAME` | `gateway-api-server` | Redis 键中的应用名 |
| `ZAUTUMN_APPLICATION_GROUP` | `lcdzzz` | Redis 键中的分组 |
| `ZAUTUMN_GATEWAY_LOGFILTER_ENABLED` | `true` | 是否启用请求转发日志 |

应用名和分组在进程启动时确定；修改后需要重启。演示 Compose 的配置不适合直接用于生产。

## 文档与协作

- [Redis 数据协议、刷新流程与已知限制](docs/redis-configuration.md)
- [开发、测试与提交指南](CONTRIBUTING.md)
- [问题反馈](https://github.com/lcdzzz/ZAutumn-gateway/issues/new/choose)
- [安全漏洞反馈政策](SECURITY.md)
- [社区行为准则](CODE_OF_CONDUCT.md)
- [变更记录](CHANGELOG.md)

## 项目结构

```text
src/main/java/indi/lcdzzz/
├── ApplicationMain.java              # 启动入口
└── gateway/
    ├── route/                        # Redis 路由加载与缓存刷新
    ├── sentinel/                     # Sentinel 路由限流数据源
    ├── apicall/                      # Java 内部调用辅助类
    ├── filters/                      # 请求转发日志
    ├── utils/                        # 应用标识和 Redis 键
    └── ZAutumnGatewayChangeGuarantor.java  # 版本轮询与消息监听
examples/                             # 可运行的本地演示配置
```

## 许可证

本项目采用 [MIT License](LICENSE)，Copyright (c) 2026 lcdzzz。第三方依赖遵循各自许可证；本项目的 MIT 许可不替代依赖自身的许可义务。
