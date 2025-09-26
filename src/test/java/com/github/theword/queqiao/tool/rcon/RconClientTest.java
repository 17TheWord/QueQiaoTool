// filepath: /workspaces/QueQiaoTool/src/test/java/com/github/theword/queqiao/tool/rcon/RconClientTest.java

import com.github.theword.queqiao.tool.config.RconConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RconClientTest {
    private static RconClient rconClient;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeAll
    public void setup() {
        RconConfig config = new RconConfig();
        config.setEnable(true);
        config.setHost("127.0.0.1"); // 请根据实际服务器修改
        config.setPort(25575); // 请根据实际服务器修改
        config.setPassword(""); // 请根据实际服务器修改
        config.setReconnectMaxTimes(1);
        config.setReconnectInterval(2);
        rconClient = new RconClient(config, logger);
        rconClient.connect();
        // 等待连接建立
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
    }

    @AfterAll
    public void teardown() {
        if (rconClient != null) {
            rconClient.stop();
        }
    }

    @Test
    public void testConnectionAndCommand() throws Exception {
        Assertions.assertTrue(rconClient.isConnected(), "Rcon 未连接成功");
        String response = rconClient.sendCommand("list"); // 发送list命令，可根据实际情况修改
        logger.info("Rcon 响应: {}", response);
        Assertions.assertNotNull(response, "Rcon 响应为空");
    }
}
