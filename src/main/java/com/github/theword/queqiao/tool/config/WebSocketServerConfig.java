package com.github.theword.queqiao.tool.config;

public class WebSocketServerConfig {

  /** 是否启用 */
  private boolean enable = true;

  /** 服务器地址 */
  private String host = "127.0.0.1";

  /** 服务器端口 */
  private int port = 8080;

  public boolean isEnable() {
    return enable;
  }

  public void setEnable(boolean enable) {
    this.enable = enable;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public WebSocketServerConfig() {}

  public WebSocketServerConfig(boolean enable, String host, int port) {
    this.enable = enable;
    this.host = host;
    this.port = port;
  }
}
