# AI Agent Scaffolding

一个基于 Spring Boot + Spring AI + Google ADK 的 Maven 多模块 Agent 开发脚手架，用于快速创建可配置的 Agent 服务。




- `api`模块：API 接口与 DTO
- `app`模块：Spring Boot 启动模块
- `domain`模块：Agent 装配、工作流、Tool/Skill/Plugin 逻辑、你的业务逻辑
- `trigger`模块：HTTP 触发层
- `infrastructure`模块：基础设施适配层
- `types`模块：通用常量、异常、枚举


1. 配置模型访问参数。
2. 在 `agenx-drawio-app/src/main/resources/agent/` 下新增一个 Agent YAML。
3. 启动 `com.xtbn.Application`。


当前工程的 Agent 通过 YAML 配置装配，配置一个yml文件（通常放在`app`模块的src/main/resources/agent/下），然后在`application-dev.yml`中添加你自定义的yml文件配置：
```yml
spring:
  config:
    import:
      - classpath:agent/agent-template.yml
      - classpath:agent/test-agent1.yml
      - classpath:agent/test-agent2.yml
      - classpath:agent/your-agent-config.yml
```
项目默认提供了一个`agent-template.yml`模板，你可以根据需要修改它。同时提供了`test-agent1.yml`和`test-agent2.yml`两个示例配置文件。
建议把每个 Agent 应用配置为一个独立 YAML 文件.



`tables` 是一个 Map，key 是当前 Agent 配置的唯一名称，例如 `demoAgent`。


运行时应用名，用于 `InMemoryRunner` 创建 session。


对外暴露的 Agent 元信息：

- `root-agent-id`：对外 ID
- `root-agent-name`：展示名称
- `root-agent-desc`：展示描述


模型 API 配置：

- `base-url`：模型服务基础地址
- `api-key`：模型密钥，强烈建议使用环境变量
- `completions-path`：聊天补全接口路径

说明：

- 当前装配逻辑通过 `OpenAiApi` + `OpenAiChatModel` 创建模型客户端
- 兼容前提是目标服务具备 OpenAI 风格接口


- `model`：模型名
- `tool-mcp-list`：MCP Tool 列表
- `tool-skill-list`：Skill Tool 列表


当前脚手架支持三类 MCP Tool 来源：

- `local`
- `sse`
- `stdio`


用于接入当前 Spring 容器里的 `ToolCallbackProvider` Bean。

示例：

```yaml
tool-mcp-list:
  - local:
      name: myToolCallbackProvider
```

要求：

- `name` 必须是 Spring 容器中的 Bean 名称
- 该 Bean 类型必须是 `ToolCallbackProvider`

当前项目默认示例在app模块的`AgentAutoConfig.java`中：

这里声明了：

```java
@Bean("myToolCallbackProvider")
public ToolCallbackProvider testTools(MyTestMcpService testService) {
    return MethodToolCallbackProvider.builder().toolObjects(testService).build();
}
```

如果你要新增本地 Tool，推荐步骤：

1. 编写一个带 `@Tool` 方法的服务类。
2. 在配置类中把它包装成 `ToolCallbackProvider` Bean。
3. 在 YAML 中通过 `local.name` 引用该 Bean 名称。


用于接入远程 MCP SSE 服务。

示例：

```yaml
tool-mcp-list:
  - sse:
      name: remote-search
      base-uri: http://localhost:8081
      sse-endpoint: /sse
      request-timeout: 5
```

字段说明：

- `name`：逻辑名称，仅用于标识
- `base-uri`：远程 MCP 服务基础地址
- `sse-endpoint`：SSE 端点，默认 `/sse`
- `request-timeout`：超时秒数

说明：

- 当前实现会把 `request-timeout` 按秒处理
- `base-uri` 也兼容直接写入包含 `sse` 的完整地址，但不推荐，建议拆开写


用于接入本地进程方式运行的 MCP Server。

示例：

```yaml
tool-mcp-list:
  - stdio:
      name: local-filesystem
      request-timeout: 5
      server-parameters:
        command: npx
        args:
          - -y
          - "@modelcontextprotocol/server-filesystem"
          - .
        env:
          NODE_ENV: production
```

字段说明：

- `name`：逻辑名称
- `request-timeout`：超时秒数
- `server-parameters.command`：启动命令
- `server-parameters.args`：命令参数列表
- `server-parameters.env`：环境变量

适用场景：

- 文件系统 MCP
- 本地代码分析 MCP
- 其他通过标准输入输出提供 MCP 协议的服务


当前脚手架通过 `SkillsTool` 装配 Skill，支持两种来源：

- `resource`
- `directory`


从 classpath 资源目录加载。

示例：

```yaml
tool-skill-list:
  - type: resource
    path: agent/skills
```

适用场景：

- 你想把 Skill 跟项目一起打包发布
- Skill 放在 `src/main/resources` 下

建议目录结构：（在`app`模块的`src/main/resources`下）

```text
agent/skills/
  example-skill/
    SKILL.md
    reference.md
    scripts/
```


从文件系统目录加载。

示例：

```yaml
tool-skill-list:
  - type: directory
    path: D:/skills/shared
```

适用场景：

- Skill 不想放进 Jar
- 多个项目共享同一份 Skill 目录

注意：

- 路径必须真实存在
- 目录为空时不会装配任何 Skill Tool


Plugin 通过 `runner.plugin-name-list` 配置，名称会映射到 Spring 容器中的 `BasePlugin` Bean。

示例：

```yaml
runner:
  agent-name: DemoWorkflow
  plugin-name-list:
    - myTestPlugin
```

要求：

- `plugin-name-list` 中每一项都必须能在 Spring 容器中找到对应 `BasePlugin`
- 名称通常取 `@Service("beanName")` 中声明的 Bean 名

当前项目示例在domain模块的`MyTestPlugin.java`中：

如果你要新增 Plugin，推荐写法：

```java
@Service("auditPlugin")
public class AuditPlugin extends BasePlugin {
    public AuditPlugin() {
        super("AuditPlugin");
    }
}
```

然后在 YAML 中引用：

```yaml
runner:
  plugin-name-list:
    - auditPlugin
```


`runtime.agents` 定义基础 Agent 节点。

示例：

```yaml
agents:
  - name: PlannerAgent
    description: Break tasks into an execution plan.
    instruction: |
      You are a planning agent.
      Analyze the user request and produce a concise plan.
    output-key: plan_result
```

字段说明：

- `name`：Agent 唯一名称，工作流和 runner 都依赖它
- `description`：Agent 描述
- `instruction`：系统提示词
- `output-key`：供后续 Agent 或工作流引用的输出变量名


`runtime.agent-workflows` 定义多 Agent 编排。

当前支持的 `type`：

- `sequential`
- `parallel`
- `loop`

示例：

```yaml
agent-workflows:
  - type: sequential
    name: DemoWorkflow
    description: Execute planning and then writing.
    sub-agents:
      - PlannerAgent
      - WriterAgent
    max-iterations: 3
```

字段说明：

- `type`：工作流类型
- `name`：工作流名称
- `sub-agents`：参与工作流的 Agent 或子工作流名称
- `description`：工作流描述
- `max-iterations`：最大迭代次数，默认 `3`


`runner` 用来指定最终运行入口。

示例：

```yaml
runner:
  agent-name: DemoWorkflow
  plugin-name-list:
    - myTestPlugin
```

字段说明：

- `agent-name`：最终执行组装的agent名称，可以是某个 Agent 名，也可以是某个 Workflow 名
- `plugin-name-list`：挂载到 `InMemoryRunner` 的插件列表


- 不要在 YAML 中写死真实 API Key，统一改为环境变量。
- 一个 YAML 文件只放一个对外 Agent 应用，便于维护。
- 本地 Tool、Plugin 的 Bean 名称保持稳定，避免 YAML 与代码脱节。


本脚手架实现了一个简单的前端chat demo，用于快速测试agent功能。
前端文件夹在`app`模块的`src/main/resources/static`下。
后端服务启动后，直接访问`http://localhost:yourport`即可。