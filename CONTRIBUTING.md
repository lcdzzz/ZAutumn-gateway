# 贡献指南

欢迎提交可复现的问题、文档修正和范围清楚的 Pull Request。维护者为 [lcdzzz](https://github.com/lcdzzz)，项目目前没有承诺响应时限。

## 开始开发

1. Fork 仓库并创建主题分支。
2. 安装 JDK 8 和 Maven 3.8.6+，执行 `java -version`、`mvn -v` 确认 Maven 使用的 JDK。
3. 执行 `mvn --batch-mode --no-transfer-progress clean verify`。默认测试不连接 Redis。
4. 按 [README](README.MD) 启动独立 Redis、演示上游和网关，验证实际转发。

代码保持 Java 8 兼容，沿用现有包结构和四空格缩进。不要把凭据、IDE 配置、日志或构建产物提交到仓库。

## 测试边界

`src/test/java/unit/TestRoute.java`、`TestSentinel.java`、`TestApiCall.java` 是历史手工集成示例，标记为 `integration`，普通构建会排除它们。其中 `TestRoute.clearAll()` 会删除对应应用的整张路由 Hash，其他方法也会写入 Redis；它们输出日志，不构成完整业务断言。

如需运行 Redis 路由/限流示例，只能连接可丢弃的独立 Redis：

```bash
docker compose up -d redis
TEST_REDIS_PORT=16379 mvn -Predis-integration -Dtest=TestRoute,TestSentinel test
```

这些测试强制使用 `zautumn-integration@test` 命名空间，默认连接本机 16379。可用 `TEST_REDIS_HOST`、`TEST_REDIS_PORT`、`TEST_REDIS_PASSWORD` 覆盖连接。不要指向共享或生产 Redis。`TestApiCall` 还需要自行准备匹配 `/api/testFlow` 的路由及上游，不包含在上述命令中。

普通构建包含对演示配置解析、路由缓存更新、无效条目过滤、刷新事件及刷新失败传播的断言。CI 在 JDK 8 和 17 上执行相同构建；17 是兼容性检查，项目编译目标仍为 Java 8。

## 提交问题和 PR

- Bug：提供提交版本、JDK/Redis 版本、脱敏配置、复现步骤、预期结果与实际结果。
- 新需求：说明场景与现有方案的问题；较大改动先开 Issue 沟通。
- PR：说明行为变化和验证命令；修复缺陷时补充能覆盖该缺陷的断言。
- 提交信息采用一行 Conventional Commits，例如 `docs: clarify Redis route configuration`。
- 提交前运行 `mvn clean verify` 和 `git diff --check`；涉及 Redis 数据协议时更新示例与文档。

不要在公开 Issue/PR 中披露尚未修复的安全漏洞，按 [安全政策](SECURITY.md) 联系维护者。参与者应遵守 [行为准则](CODE_OF_CONDUCT.md)。提交贡献即表示你有权提供该内容，并同意贡献按本项目 MIT 许可证分发。
