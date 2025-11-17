# 鹊桥工具包

一个用于 Minecraft 服务端（插件 / 模组）与外部应用之间进行实时通信与事件分发的轻量级工具库。

- WebSocket 正反向连接。
- 统一的 JSON 请求 / 响应协议（自定义轻量协议）。
- 游戏内事件实时推送（`聊天`、`加入`、`离开`、`死亡`、`命令`、`成就`/`进度` 等）。
- 消息发送能力（`广播`、`ActionBar`、`Title` & `Subtitle`、`私聊`）。
- `Rcon` 支持。

## 快速开始

1. 在服务端启动完成后阶段调用：
   ```java
   GlobalContext.init(
       /* isModServer */ true,
       /* serverVersion */ "1.20.1",
       /* serverType */ "fabric",
       /* handleApiImpl */ new YourHandleApiImpl(),
       /* handleCommandReturnMessageImpl */ new YourCmdReturnImpl()
   );
   ```
2. 接口实现：
    - `com.github.theword.queqiao.tool.handle.HandleApiService`： 实现发送广播、title、actionbar、私聊等实际逻辑（调用原生
      API）。
    - `com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService`： 用于在控制台或指令执行者上下文中返回信息与权限判定。
    - `com.github.theword.queqiao.tool.command.subCommand`：实现各 `XxxAbstract` 子命令并注册。
3. 在服务端关闭前调用：
   ```java
   GlobalContext.shutdown();
   ```
4. 外部应用通过 WebSocket 发送 JSON 请求（含 `api`、`data`、可选 `echo`）。
5. 订阅所需事件：收到的事件是服务端主动推送。

## 接口说明

- `V2` 完整协议：[`ApiFox`](https://queqiao.apifox.cn/)：适用于 鹊桥 `0.3.0` 及以上版本
- `V1` 协议：[`ApiFox`](https://rxylncffzr.apifox.cn)：适用于 鹊桥 `0.2.7` 及以下版本
- `V1` 事件：[`QueQiao GitHub Wiki`](https://github.com/17TheWord/QueQiao/wiki/4.-%E5%9F%BA%E6%9C%AC%E4%BA%8B%E4%BB%B6%E7%B1%BB%E5%9E%8B)适用于 鹊桥 `0.2.7` 及以下版本

## 配置文件说明

文件：[`src/main/resources/config.yml`](./src/main/resources/config.yml)

## Rcon 支持

> Minecraft 原生的远程控制协议，允许通过网络发送命令到 Minecraft 服务器并获取响应。

- 依赖 `org.glavo:rcon-java:3.0`
- 为节约端口资源，直接将 `Rcon` 功能集成在本工具包中。
- 开放 `send_rcon_command` 接口供外部应用调用。

## 开发与测试

1. 克隆项目

    ```shell
    git clone https://github.com/17TheWord/QueQiao.git
    ```

2. `JDK`：为支持 `1.7.10` - `1.12.2`，项目使用 `JDK 8`。

3. 使用 `IDE` 打开项目（推荐 `IntelliJ IDEA`），或使用 `gradlew`。

    ```bash
    ./gradlew.bat test
    ./gradlew.bat build
    ```

## 构建与依赖

- 前往 `Release` 查看最新版本。
- 项目使用 GitHub Packages ，需配置凭证，参考 [GitHub Packages](https://docs.github.com/zh/packages) 配置。
- 环境变量
    - `GH_USERNAME`：GitHub 用户名。
    - `PACKAGE_READ_ONLY_TOKEN`：GitHub 个人访问令牌。

Gradle (Kotlin DSL)：

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/17TheWord/QueQiaoTool")
        credentials {
            username = System.getenv("GH_USERNAME")
            password = System.getenv("PACKAGE_READ_ONLY_TOKEN")
        }
    }
}

dependencies {
    implementation("com.github.theword.queqiao:queqiao-tool:0.3.7")
}
```

## 社群

- [`Discord`](https://discord.gg/SBUkMYsyf2)

## 许可证

本项目使用 [MIT](./LICENSE) 许可证开源。
