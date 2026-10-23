# 变更记录

记录面向使用者的主要变化。`Unreleased` 表示默认分支待发布内容，不代表已发行版本。

## Unreleased

### 新增

- MIT 开源许可证、贡献指南、社区行为准则和安全报告渠道。
- 本地 Redis/HTTP 上游演示、路由和限流 JSON 示例。
- GitHub Issue/PR 模板、JDK 8/17 CI 构建与 Dependabot 配置。
- 无 Redis 依赖的路由配置及刷新行为自动化测试。

### 调整

- 重写快速开始及 Redis 配置协议，澄清当前功能与已知限制。
- 为 Maven 补充项目元数据、JUnit 5 测试插件及可执行 Spring Boot JAR 打包。
- 历史 Redis 集成示例默认排除，通过 Maven profile 显式启用并使用独立测试命名空间。

## 历史代码

此前代码没有 GitHub Release；历史变化请查看 [提交记录](https://github.com/lcdzzz/ZAutumn-gateway/commits/master/)。
