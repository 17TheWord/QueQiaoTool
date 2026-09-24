# Changelog

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

> 说明：本文件记录**面向使用者的行为变更**。内部重构若不影响公开行为则不在此列出。
> `[Unreleased]` 段落中的内容将在发布时改为对应版本号。

---

## [Unreleased]

本段包含 WebSocket 生命周期（WS-A）与协议分发（WS-B）两批改造。
**其中「破坏性变更」一节请务必阅读**——协议状态码与部分响应字段发生了变化。

### ⚠️ 破坏性变更

#### 1. 协议响应状态码变化

状态码此前无法区分"调用方请求错误"与"服务端内部错误"，现已按语义重新划分：

| 场景 | 旧状态码 | 新状态码 |
| --- | --- | --- |
| 请求体不是合法 JSON | `500` | `400` |
| 请求体为字面量 `null` | `500` | `400` |
| 缺少 / 空白 `api` 字段 | `404`（提示"未知 API：null"） | `400` |
| `api` 未注册 | `404` | `404`（不变） |
| Rcon 未启用 / 未连接 | `400` | **`503`** |
| Rcon 命令已下发但执行失败 | `400` | **`500`** |
| Rcon 命令为空 | 未校验 | `400` |
| Title 的 `fade_in` / `stay` / `fade_out` 为负数 | 未校验 | `400` |
| Title 的时长超过 72000 ticks（1 小时） | 未校验 | `400` |
| `send_private_msg` 的 `nickname` 为纯空白 | 视为有效 | `400` |

**升级指引**：若你的客户端按状态码分支处理，请更新判断逻辑。
特别注意 Rcon 相关的 `400` 已拆分为 `503`（服务不可用，可重试）与 `500`（服务端错误）。

#### 2. 错误响应不再回传原始请求体

解析失败时，响应 `data` 中的 `rawJsonMessage` 字段**已移除**。

旧行为会把整个请求体原样塞回响应，既放大响应体积，也可能把敏感内容回显给调用方。
调试时请开启 `debug` 配置，日志中会输出**已脱敏并截断**的请求内容。

**升级指引**：若客户端依赖 `data.rawJsonMessage` 排查问题，请改用服务端 debug 日志。

#### 3. `get_status` 结果新增最长 2 秒缓存

`get_status` 会在连接读线程上同步执行一次 Minecraft Server List Ping（socket 超时 3 秒）。
为避免高频轮询反复占用读线程，现加入 **2 秒 TTL 快照缓存**。

**影响**：2 秒内的重复请求返回同一份快照，`timestamp` 最多滞后 2 秒。
**升级指引**：若需要"绝对实时"的状态，请把轮询间隔设为大于 2 秒，或接受该滞后。

#### 4. 构造器签名变更（源码级）

仅影响**直接构造**这些类的代码；通过 `GlobalContext.init(...)` 使用的平台实现不受影响。

| 类 | 变化 |
| --- | --- |
| `WsClient` | 新增 `ScheduledExecutorService` / `ReconnectPolicy` / `HandleProtocolMessage` 参数；移除 `gson`、`reconnectMaxTimes`、`reconnectInterval` |
| `WsServer` | 以 `HandleProtocolMessage` 替换 `gson` 参数 |
| `WebsocketManager` | 新增 `HandleProtocolMessage` 参数 |
| `ProtocolRouter` | 新增 `Logger` 参数（不再依赖全局状态） |
| `AbstractProtocolHandler` 及全部 7 个 handler | 新增 `Logger` 参数 |

### 新增

- `ReconnectPolicy`：重连退避策略纯组件，无网络与线程依赖，可独立测试。
- `ReconnectReason`：重连原因枚举（`REMOTE_CLOSE` / `MANUAL`）。
- `WebSocketUrlNormalizer`：WebSocket URL 归一化（trim / 去空 / 去重 / `ws://` `wss://` scheme 校验）与日志脱敏。
- `LogSanitizer`：日志脱敏工具，按字段名递归遮蔽 `access_token` / `authorization` / `token` / `password` / `passwd` / `secret` / `api_key` / `apikey`，并按 1 KB 截断。
- `ProtocolConstants.Status.SERVICE_UNAVAILABLE`（`503`）。
- `ProtocolException.internalError(String, Object)` / `ProtocolException.serviceUnavailable(String, Object)`。
- `RconException.Kind`（`DISABLED` / `DISCONNECTED` / `COMMAND_FAILED`），用于区分 Rcon 失败性质。
- `Tool.isDebugEnabled()` 由 private 改为 public，便于调用方在构造昂贵的日志参数前先行判断。
- `WsClient#isReconnectInProgress()` / `getReconnectAttempts()` / `isStopped()`：重连状态诊断接口。

### 变更

- **`WsServer` 的连接丢失检测周期显式设为 60 秒**，不再依赖 Java-WebSocket 的默认值
  （该检测会 ping 客户端，超时未收到 pong 时关闭连接）。
- **`WsServer` 构造器把 `serverName` / `accessToken` 的 `null` 归一化为空串**。
  `accessToken` 为空表示不鉴权——即传 `null` 与传 `""` 现在等价，不会再因 NPE 进入异常路径。
- **明确协议只支持文本帧**：二进制帧由 Java-WebSocket 的空实现静默忽略，本项目有意不覆写
  （覆写只能多打一条日志、不改变行为，反而会在客户端持续发送二进制帧时刷屏）。
  若排查"客户端称已发送但服务端无响应"，这是需要确认的方向之一。
- **`GlobalContext.init()` 现在幂等**：若已初始化，会**先关闭旧实例**再创建新实例。
  此前重复初始化会直接覆盖运行时引用，导致旧实例持有的 WebSocket 连接、共享重连调度器、
  Rcon 连接与线程变成无法再关闭的孤儿对象。对"插件热重载"这类场景现在是正确行为。
- **未初始化 / 已关闭状态下上下文可用**：`GlobalContext.getConfig()` 与 `getLogger()`
  不再返回 `null`，而是返回默认配置与不输出内容的 NOP 日志；
  `shutdown()` / `sendEvent()` 在该状态下为安全空操作。
  依赖"未初始化时读配置会抛 NPE"来探测状态的代码需调整。
- **重连机制统一**：自动重连与手动重连共用同一套 pipeline（`requestReconnect`），
  引入代际号（generation）与 `reconnectInProgress` 非阻塞互斥，保证同一 Client 任意时刻
  最多一个待执行、最多一个正在执行的重连。
- **共享重连调度器**：由 `WebsocketManager` 持有单个调度器（corePoolSize = 2、daemon、命名线程），
  取代"每个 Client 一条调度线程"。调度器仅在 Manager 永久销毁时关闭。
- **连接成功时清理**：`onOpen` 会取消待执行的重连任务、推进代际号并重置连续失败计数，
  避免"迟到的重连任务"把刚建立的健康连接拆掉。
- **重连判据**：不再依赖 Java-WebSocket 的 `remote` 参数（连接失败时它也是 `false`），
  改用 `stopped` 表达"是否仍需要维持连接"。
- **`connectionLostTimeout` 显式设置**为 60 秒，不再依赖库默认值。
- **协议层不再依赖全局状态**：`ProtocolRouter` 与各 handler 的日志实现改为构造器注入，
  协议分发可脱离 `GlobalContext` 独立测试。
- **`sendEvent` 分发**：先取接收方快照再序列化；无任何接收方时跳过序列化。
  （序列化与分发仍在调用线程同步完成——实测单次约 1~2.3 µs，未引入异步队列。）
- **日志级别调整**：`get_status` 请求日志由 `INFO` 降为 `DEBUG`（该接口可能被高频轮询）。
- **Rcon 命令日志脱敏**：`INFO` 级别只记录命令长度，完整命令内容仅在 `debug` 级别输出。
- **Rcon 日志来源修正**：错误日志按实际来源输出 `http` / `websocket`，不再固定写 `webSocket`。
- **URL 归一化**：配置中的 URL 会 trim、去空、去重，并校验 scheme；
  非 `ws://` / `wss://` 的地址在加载阶段即被拒绝并告警（不再拖到连接阶段才报错）。
- **`EmptyPayload` 的 `data` 字段保持容忍**：对无负载的 api（如 `get_status`），
  `data` 会被忽略而不校验——客户端习惯性发送 `"data": {}` 或 `"data": null`，拒绝只会造成无谓的兼容性破坏。
- **`nickname` 判空改为 `trim()` 后判断**，纯空白字符串视为未提供；
  `nickname` 与 `uuid` 同时提供时的优先级仍由平台实现决定。

### 修复

- **握手异常导致鉴权旁路**（严重，安全）：Java-WebSocket 的 `WebSocketImpl.open()` 会
  **吞掉 `onOpen` 抛出的 `RuntimeException` 并让连接保持 `OPEN`**（只上报、不关闭、不重抛）。
  因此一旦 `onOpen` 内部抛异常，未通过鉴权的连接会被保留、后续消息仍被正常处理。
  现已改为：`onOpen` 内部兜底，**任何异常都主动关闭连接**，绝不让异常逃出。
  （该库行为已用一次性实验实证：客户端未被关闭、服务端仍持有该未鉴权连接。）
- **服务端级错误时日志抛 NPE**：库在 selector 等致命错误时会以 `onError(null, e)` 回调，
  此前 `getClientAddress` 直接对 null 取远端地址会抛 `NullPointerException`。
  现已做空值防护，并分别返回 `<server>`（无具体客户端）与 `<unknown>`（无法获取远端地址）占位符。
  同时地址获取改用 `InetSocketAddress#getHostString()` / `getPort()`，
  不再依赖 `InetSocketAddress.toString()` 的格式做字符串清理。
- **`onError` 日志出现无意义的 `null`**：异常 `getMessage()` 为 null 时现回退为异常类名。
- **重复初始化泄漏资源**（严重）：`GlobalContext.init()` 此前无幂等保护，
  重复调用会覆盖运行时引用，旧实例的 WebSocket 连接、共享调度器、Rcon 连接与线程永久泄漏。
- **未初始化时上下文方法直接崩溃**（严重）：运行时空对象此前是"所有字段为 null"的失效对象，
  导致 `GlobalContext.shutdown()` / `sendEvent()` 在未初始化时抛 `NullPointerException`。
  现已改为真正可用的最小运行时。
- **运行时的跨线程可见性缺陷**（严重）：`GlobalContext.runtime` 此前非 `volatile`，
  且运行时字段既非 `final` 也无 `volatile`，由初始化线程写入、由游戏线程与 WebSocket 线程读取，
  可能读到旧引用或半初始化对象（且难以复现）。现已用 `volatile` + `final` 显式声明修复。
- **`Config.getIgnoredCommands()` 可能返回 null**：该字段此前无初始值，
  一旦配置加载中途失败便会保持 null，导致 `Tool.isIgnoredCommand` 抛 `NullPointerException`。
  现已初始化为空集合，并在默认配置中预置注册/登录命令。
- **重连退避溢出**（严重）：原实现 `reconnectInterval * (1L << reconnectTimes)` 在
  `reconnectTimes >= 63` 时移位溢出为负数，负数延迟被调度器当作"立即执行"，
  会形成**无间隔重连风暴**。现改为无位移的封顶翻倍算法，并保证延迟恒在合法区间。
- **连接失败后不自动重连**（严重）：原 `onClose` 中的 `if (remote && !stopped)` 判定
  对"服务端未监听"这类最常见的失败恒为 `false`，自动重连完全依赖 `onError` 兜底。
  现已修正为以 `stopped` 为唯一判据。
- **一次故障被安排两次重连**：`onError` 与 `onClose` 此前各自调度重连，导致重试计数被双倍消耗、
  退避曲线失真。现 `onError` 只记录错误，`onClose` 作为自动重连唯一入口。
- **停止后仍会重连**：`stopWithoutReconnect` 与待执行任务之间存在竞态，现已通过
  代际号失效 + 取消任务 + 停止标志三者共同保证。
- **每个 WsClient 泄漏一条非 daemon 线程**：`Executors.newSingleThreadScheduledExecutor()`
  为每实例创建且从不命名、异常路径不回收，非 daemon 还会阻止 JVM 退出。已改为共享 daemon 调度器。
- **`init()` 重复调用泄漏**：`WebsocketManager.start()` 此前不幂等，重复调用会追加重复 Client；
  现 `start()` / `stop()` 均幂等。
- **单个 endpoint 失败会阻断其余 endpoint**：`startClients` 此前只捕获 `URISyntaxException`，
  其它异常会中断整个循环；现每个 endpoint 独立隔离，失败实例会被回收。
- **业务异常打断连接**：`WsClient.onMessage` 此前无异常防护，
  业务层抛出的 `RuntimeException` 会穿透到 Java-WebSocket 读循环，
  被其捕获后执行 `closeConnection(ABNORMAL_CLOSE)`——**一条异常消息即可打断连接**。
  现已在 callback 内隔离，异常只记日志、不上抛。
- **认证失败日志泄漏 token**：`WsServer` 此前会把客户端提交的 `Authorization` 值原样写入日志。
  现已移除该值，只记录来源地址（`Authorization` 字段名仍保留以便定位问题）。
- **`Tool.debugLog` 空指针**：未初始化全局上下文时 `GlobalContext.getConfig()` 返回 null，
  导致 `debugLog` 抛 `NullPointerException`。已加空值防护。
- **解析失败被误判为服务端错误**：见「破坏性变更」第 1 条。

### 移除

- 移除 `WsServer.broadcast(String)` 的**纯透传覆写**（方法体只有 `super.broadcast(text)`，无任何附加逻辑）。
- 删除两个无有效断言的测试类：`WsClientTest`、`WsServerTest`。
  前者只断言 JDK 的 `URLEncoder` 行为（对 `WsClient` 零覆盖），
  后者同样只断言 `URLEncoder` / `URLDecoder`，且其 `testOnOpen` 仅在固定端口 25565
  启动服务端后断言非空、从不关闭。已由真实 socket 集成测试取代。

### 测试

- 测试用例数由 68 增至 98，项目整体行覆盖率由 37.7% 提升至 47.6%。
- 新增真实 socket 集成测试（随机端口 + `CountDownLatch`，不使用 `Thread.sleep` 做断言）：
  重连回归、停止隔离、多客户端共享调度器、线程数上界与 daemon 校验、握手认证矩阵。
- 新增协议分发矩阵测试与日志脱敏测试。
