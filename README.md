# agenx-drawio

`agenx-drawio` 是一个面向 draw.io 图表生成场景的多 Agent 服务端项目。它基于 Spring Boot、Spring AI 和 Google ADK，提供可配置的 Agent 装配能力，并内置会话管理、流式响应、治理、安全过滤和监控能力，目标不是做通用聊天，而是把“结构化需求 -> 图表生成 Agent”这条链路工程化。

## STAR

### Situation

很多 Agent Demo 只能证明模型能回答问题，但一旦进入真实业务，就会出现几个典型问题：

- Agent 定义散落在代码里，调整模型、工具、工作流都要改代码
- 图表生成这类任务对结构、格式、布局约束更高，普通聊天链路不够稳定
- 缺少会话持久化、治理限流、敏感信息处理和观测埋点，难以落地到服务端

### Task

这个项目要解决的核心任务是：把 draw.io 图表生成能力做成一个可扩展、可治理、可观测的后端服务，而不是一次性的提示词实验。

具体目标包括：

- 用 YAML 而不是硬编码来装配 Agent、Workflow、Tool、Skill 和 Plugin
- 支持面向图表场景的不同 Agent 配置，例如流程图、时序图、规格抽取与润色
- 提供标准 HTTP / SSE 接口，便于前端或其他系统接入
- 在服务端补齐 session、memory、治理、安全和审计能力

### Action

项目当前实现的主要能力包括：

- 多模块 DDD 风格结构
  - `api`、`app`、`domain`、`trigger`、`infrastructure`、`types` 分层清晰，便于扩展业务能力与基础设施适配。
- YAML 驱动的 Agent 装配
  - Agent、模型、工作流、MCP Tool、Skill Tool 和 Plugin 都可以通过资源配置装配，降低新增 Agent 成本。
- 面向 draw.io 的专用 Agent 配置
  - 内置 `drawio-fast`、`drawio-spec`、`drawio-polish`、`drawio-max`、`drawio-degrade` 等配置，覆盖生成、约束、润色和兜底场景。
- 会话与历史管理
  - 提供创建会话、切换会话、查询历史、流式聊天等接口，支持持续对话和图表迭代。
- 图表生成链路约束
  - 内置 draw.io XML 格式校验、布局校验、重试提示修正等支持逻辑，降低模型输出不可用 XML 的概率。
- 安全与治理插件
  - `GovernancePlugin` 负责黑名单、限流、并发控制和配额。
  - `PrivacyPlugin` 负责对用户消息中的隐私信息做脱敏。
  - `SensitiveWordPlugin` 负责敏感词检测、替换和审计落库。
- 观测与日志
  - `ObservabilityPlugin` 提供请求、模型、工具调用相关的指标与日志，方便排障和容量评估。
- 前后端联调能力
  - 自带一个轻量前端 demo 和 OpenAPI 描述文件，可直接用于功能验证和接口联调。

### Result

这个项目当前已经具备从“定义 Agent”到“对外提供图表生成服务”的完整基础骨架，适合作为以下场景的起点：

- 搭建企业内部的 draw.io 图表生成服务
- 验证多 Agent 编排在结构化制图任务中的工程化方案
- 在 Spring Boot 体系内集成 MCP Tool、Skill 和治理插件
- 作为可二次开发的 Agent 后端模板，继续扩展更多垂直场景

它的价值不在于单次回答效果，而在于把 Agent 服务的配置、治理、安全和可观测性一并落到了工程结构里。

## Core Capabilities

- `HTTP + SSE` 接口，支持同步和流式响应
- `Session` 持久化与历史消息管理
- `MCP Tool`、`Skill Tool`、`Plugin` 可插拔扩展
- draw.io XML 生成、校验、修正和布局约束
- 限流、并发控制、配额、黑名单治理
- 隐私脱敏、敏感词审计
- 指标监控与结构化日志

## Repository Structure

- `agenx-drawio-api`：接口定义与 DTO
- `agenx-drawio-app`：Spring Boot 启动与资源配置
- `agenx-drawio-domain`：Agent 装配、工作流、插件与核心业务逻辑
- `agenx-drawio-infrastructure`：数据库、Redis、安全与仓储适配
- `agenx-drawio-trigger`：HTTP 触发层
- `agenx-drawio-types`：通用常量、异常、枚举

## Notes

- 仓库中的敏感配置应始终通过环境变量注入，不要提交真实密钥。
- 文档中的口令和账号均应视为占位符，公开使用前请替换。
