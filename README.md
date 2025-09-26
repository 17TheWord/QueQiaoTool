# 鹊桥 工具包

## 介绍

- 封装 `Websocket Client`、`Websocket Server`
- 仿 `OneBot` 风格协议
- 接收消息后由具体模组根据服务端API通过继承接口实现
- 支持多种消息类型
    - [x] Broadcast
    - [x] ActionBar
    - [x] Title & Subtitle
    - [x] Private
- 多种玩家事件监听
    - [x] 聊天
    - [x] 加入
    - [x] 离开
    - [x] 死亡（死亡信息为英文）
    - [x] 命令

- 连接方式
  - `正向 Websocket`：根据 [`websocket_server`](./src/main/resources/config.yml#L15) 配置项自行设置
  - `反向 Websocekt`：根据 [`websocket_client`](./src/main/resources/config.yml#L21) 配置项自行设置
    - 支持多个反向连接
    - 例如：`ws://127.0.0.1:8080/minecraft/ws` 用于通过 `nonebot-adapter-minecraft` 连接 `nonebot`
    - 例如：`ws://127.0.0.1:8081/minecraft/ws` 用于连接云崽的 `mc-plugin` 插件

## 配置文件

- [`config.yml`](./src/main/resources/config.yml)

## 参与 & 开发

- [`Wiki`](https://github.com/17TheWord/QueQiaoTool/wiki) 帮助

## 许可证

本项目使用 [MIT](./LICENSE) 作为开源许可证。
