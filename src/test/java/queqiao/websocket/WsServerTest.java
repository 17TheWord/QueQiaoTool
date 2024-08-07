package queqiao.websocket;

import com.github.theword.queqiao.tool.websocket.WsServer;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.github.theword.queqiao.tool.utils.Tool.logger;

class WsServerTest {

    @Test
    void testUnicodeServerName() throws UnsupportedEncodingException {
        System.out.println(URLEncoder.encode("服务器", StandardCharsets.UTF_8.toString()));
    }

    @Test
    void testDecodeServerName() throws UnsupportedEncodingException {
        System.out.println(URLDecoder.decode("%E6%9C%8D%E5%8A%A1%E5%99%A8", StandardCharsets.UTF_8.toString()));
    }

    @Test
    void testOnOpen() {
        logger = LoggerFactory.getLogger(WsServerTest.class);
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 25565);
        WsServer wsServer = new WsServer(inetSocketAddress);
        wsServer.start();
        try {
            Thread.sleep(3000);
            wsServer.stop();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}