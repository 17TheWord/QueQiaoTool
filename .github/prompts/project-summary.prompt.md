---
description: "总结 QueQiaoTool 项目现状、架构、协议能力与维护关注点"
name: "QueQiaoTool Project Summary"
agent: "agent"
---
# QueQiaoTool 项目总结

## 项目定位

QueQiaoTool 是 QueQiao 生态中的通用 Java 工具库，面向 Minecraft 服务端插件/模组平台适配层提供统一能力。它抽象并集中实现：

- WebSocket Server / Client 正反向连接。
- JSON 请求、响应与事件推送协议。
- 配置读取与默认配置管理。
- 玩家事件模型与消息 payload。
- 广播、私聊、Title、ActionBar、Rcon、服务器状态等 API 的协议分发。
- 命令框架、重载、客户端重连、服务端信息查询。
- 多语言/翻译辅助与服务器状态采集。

QueQiaoTool 本身不直接依赖某个 Minecraft 服务端 API，而是通过接口由 QueQiao 各平台实现具体游戏内行为。

## 技术栈

- 语言：Java
- 目标版本：Java 8
- 构建：Gradle Kotlin DSL
- 测试：JUnit 5
- 覆盖率：JaCoCo
- 发布：Maven Publish 到 GitHub Packages
- 主要依赖：
  - `com.google.code.gson:gson`
  - `org.java-websocket:Java-WebSocket`
  - `org.yaml:snakeyaml`
  - `org.slf4j:slf4j-api`
  - `org.glavo:rcon-java`
  - `commons-io:commons-io`

## 核心包结构

根包：`src/main/java/com/github/theword/queqiao/tool/`

- `GlobalContext.java`：全局上下文与生命周期入口，负责加载配置、初始化 WebSocket、Rcon、翻译、状态采集，并暴露事件发送与重载逻辑。
- `config/`：配置模型，包括 `Config`、`CommonConfig`、`WebSocketServerConfig`、`WebSocketClientConfig`、`RconConfig`、`SubscribeEventConfig`。
- `websocket/`：`WsServer` 与 `WsClient`，实现服务端监听与客户端反向连接。
- `utils/`：`WebsocketManager`、`Tool`、`GsonUtils`、`ServerStatusCollector`、`SystemMetricsCollector`、`MinecraftPingClient` 等辅助能力。
- `handle/`：协议处理与平台能力接口。
  - `HandleProtocolMessage`：解析 `api`、`data`、`echo` 并分派到具体 API。
  - `HandleApiService`：由平台实现的游戏内消息发送能力。
  - `HandleCommandReturnMessageService`：由平台实现的命令反馈与权限能力。
- `payload/`：请求载荷模型，如 `BasePayload`、`MessagePayload`、`PrivateMessagePayload`、`TitlePayload`、`CommandPayload`。
- `response/`：响应模型，如 `Response`、`PrivateMessageResponse`、`ResponseEnum`。
- `event/`：玩家事件模型。
  - 顶层事件：`PlayerChatEvent`、`PlayerCommandEvent`、`PlayerDeathEvent`、`PlayerJoinEvent`、`PlayerQuitEvent`、`PlayerAchievementEvent`。
  - `base/`、`player/`、`model/`：公共事件基类、玩家模型、死亡/成就等嵌套模型。
- `command/`：命令框架。
  - `RootCommand`、`SubCommand`、`CommandExecutorHelper`
  - `subCommand/client/`：重连、全部重连、列表等命令。
  - `subCommand/server/`：服务器信息命令。
- `rcon/`：`RconClient`，封装原生 Rcon 调用。
- `localize/`：`LanguageService`，支持本地翻译文件。
- `constant/`：基础常量、命令常量、服务端类型、WebSocket 常量消息。

## 生命周期

典型接入流程：

1. 平台层在服务端启动完成后调用 `GlobalContext.init(...)`。
2. `GlobalContext` 加载 YAML 配置、初始化 Gson、日志、平台接口、WebSocket 管理器、Rcon 客户端、翻译服务和状态采集。
3. 平台层监听 Minecraft 事件，构造 `BaseEvent` 子类后调用 `GlobalContext.sendEvent(...)`。
4. 外部应用通过 WebSocket 发送 JSON 请求，由 `HandleProtocolMessage` 分派至平台层 `HandleApiService`。
5. 服务端关闭前调用 `GlobalContext.shutdown()`，关闭 WebSocket、Rcon 与翻译服务。

## 协议能力

请求基本结构：

- `api`：接口名。
- `data`：接口参数。
- `echo`：可选回显字段。

主要 API：

- `broadcast` / `send_msg`：广播消息。
- `send_private_msg`：发送私聊消息。
- `send_title`：发送 Title / Subtitle。
- `send_actionbar`：发送 ActionBar。
- `send_rcon_command`：执行 Rcon 命令。
- `get_status`：返回服务器状态快照。
- `send_command`：当前代码中标记为暂不支持。

主要事件：

- 玩家聊天
- 玩家命令
- 玩家死亡
- 玩家加入
- 玩家退出
- 玩家成就/进度

## 配置文件

默认配置文件：`src/main/resources/queqiao/config.yml`

关键配置项：

- `enable`：是否启用。
- `debug`：是否输出调试日志。
- `server_name`：服务器名称。
- `access_token`：连接鉴权 token。
- `message_prefix`：游戏内消息前缀，支持文本或 Minecraft JSON 组件。
- `enable_translation`：是否启用翻译。
- `websocket_server`：WebSocket Server 地址与端口。
- `websocket_client`：反向连接 URL 列表、重连间隔、最大重连次数。
- `rcon`：Rcon 开关、端口、密码。
- `subscribe_event`：玩家事件订阅开关。
- `ignored_commands`：忽略的命令前缀列表。

## 测试与质量保障

测试目录：`src/test/java/com/github/theword/queqiao/tool/`

已有测试覆盖方向：

- 配置模型：`ConfigTest`、`WebSocketServerConfigTest`、`WebSocketClientConfigTest`、`RconConfigTest`、`SubscribeEventConfigTest`
- WebSocket：`WsServerTest`、`WsClientTest`
- 工具：`ToolTest`、`GsonUtilsTest`
- 响应模型：`ResponseTest`、`ResponseEnumTest`、`PrivateMessageResponseTest`
- Payload：`BasePayloadTest`、`MessagePayloadTest`、`PrivateMessagePayloadTest`、`TitlePayloadTest`、`CommandPayloadTest`
- 事件模型：玩家聊天、命令、死亡、加入、退出、成就事件测试
- 全局上下文：`GlobalContextTest`

构建任务：

- `./gradlew.bat test`
- `./gradlew.bat build`
- `./gradlew.bat jacocoTestReport`

发布配置位于 `build.gradle.kts`，会生成主 Jar、Sources Jar、Javadoc Jar，并发布到 GitHub Packages。

## 关键文件

- `README.md`：项目说明、接入方式、协议文档入口、依赖方式。
- `build.gradle.kts`：构建、测试、JaCoCo、发布配置。
- `gradle.properties`：项目版本与依赖版本。
- `settings.gradle.kts`：项目名 `queqiao-tool`。
- `src/main/resources/queqiao/config.yml`：默认配置模板。
- `src/main/java/com/github/theword/queqiao/tool/GlobalContext.java`：生命周期核心。
- `src/main/java/com/github/theword/queqiao/tool/handle/HandleProtocolMessage.java`：协议分发核心。
- `src/main/java/com/github/theword/queqiao/tool/utils/WebsocketManager.java`：WebSocket 管理核心。
- `src/main/java/com/github/theword/queqiao/tool/rcon/RconClient.java`：Rcon 封装。

## 优势

- 把跨平台公共逻辑从 QueQiao 各 loader 中抽离，降低平台适配重复成本。
- 接口边界清晰：平台只需实现消息发送、命令反馈、权限等与服务端 API 相关的部分。
- 协议模型、事件模型、payload、response 结构较完整。
- 有测试、JaCoCo、Javadoc、Sources Jar 和 GitHub Packages 发布流程。
- 保持 Java 8 兼容，适合支持旧版 Minecraft。

## 风险与维护关注点

- `GlobalContext` 静态全局状态较多，测试隔离、重载与并发场景需要谨慎。
- 协议分发集中在 `HandleProtocolMessage` 的 `switch` 中，API 增多后可维护性会下降。
- Java 8 兼容限制了部分现代库和语言特性的使用。
- WebSocket、Rcon、翻译、状态采集均属于运行时敏感能力，应持续关注异常处理与资源关闭。
- 需要确保 README 中示例版本、`gradle.properties` 的 `projectVersion` 与 QueQiao 的 `tool_version.txt` 保持一致。

## 后续维护建议

- 为每个 API 增加协议级单元测试，尤其是错误请求、缺字段、鉴权失败和 Rcon 异常。
- 将 `HandleProtocolMessage` 的 API 分发逐步拆成可注册的处理器，降低单类复杂度。
- 为 `GlobalContext` 增加测试辅助重置方法或实例化上下文，提升测试隔离。
- 明确 WebSocket 鉴权、心跳、重连、广播失败的行为规范。
- 为外部接入者补充完整 JSON 示例与响应示例。
