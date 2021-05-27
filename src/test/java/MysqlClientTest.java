import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xinc.mysql.codec.*;


import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
@Slf4j
class MysqlClientTest {

    static String serverHost;
    static int serverPort;
    static String user;
    static String password;
    static String database;

    static SimpleClient client;

    @Test
    public void handshake() throws InterruptedException, IOException {
        try (Connection connection = client.connect()) {
            final Handshake handshake = (Handshake) connection.pollServer();

            final HandshakeResponse response = HandshakeResponse
                    .create()
                    .addCapabilities(CLIENT_CAPABILITIES)
                    .username(user)
                    .addAuthData(MysqlNativePasswordUtil.hashPassword(password, handshake.getAuthPluginData()))
                    .database(database)
                    .authPluginName(Constants.MYSQL_NATIVE_PASSWORD)
                    .build();
            connection.write(response).sync();

            System.in.read();
        }
    }

    @BeforeAll
    public static void init() {
        serverHost = "127.0.0.1";
        serverPort = Integer.getInteger("mysql.port", 13306);
        user = "root";
        password = "123456";
        database = "test";

        client = new SimpleClient();
    }

    private static String getProperty(String property) {
        final String value = System.getProperty(property);
        return value;
    }

    @AfterAll
    public static void cleanup() {
        if (client != null) {
            log.info("关闭数据库");
            client.close();
        }
    }

    protected static final EnumSet<CapabilityFlags> CLIENT_CAPABILITIES = CapabilityFlags.getImplicitCapabilities();
    static {
        CLIENT_CAPABILITIES.addAll(EnumSet.of(CapabilityFlags.CLIENT_PLUGIN_AUTH, CapabilityFlags.CLIENT_SECURE_CONNECTION, CapabilityFlags.CLIENT_CONNECT_WITH_DB));
    }

    protected static class SimpleClient implements AutoCloseable {

        private final EventLoopGroup eventLoopGroup;
        private final Bootstrap bootstrap;

        public SimpleClient() {
            eventLoopGroup = new NioEventLoopGroup();
            bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    CapabilityFlags.setCapabilitiexinctr(ch, CLIENT_CAPABILITIES);
                    ch.pipeline().addLast(new LoggingHandler());
                    ch.pipeline().addLast(new MysqlServerConnectionPacketDecoder(0));
                    ch.pipeline().addLast(new MysqlClientPacketEncoder());
                }
            });

        }

        public void close() {
            eventLoopGroup.shutdownGracefully();
        }

        public Connection connect() {
            return new Connection(bootstrap.connect(serverHost, serverPort));
        }

    }

    protected static class Connection implements AutoCloseable {
        private final Channel channel;
        private final BlockingQueue<MysqlServerPacket> serverPackets = new LinkedBlockingQueue<MysqlServerPacket>();

        private Handshake handshake;

        Connection(ChannelFuture connectFuture) {
            try {
                connectFuture.addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        future.channel().pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                log.info("收到服务器数据");
                                if (msg instanceof Handshake) {
                                    CapabilityFlags.getCapabilitiexinctr(ctx.channel()).retainAll(((Handshake) msg).getCapabilities());
                                }
                                serverPackets.add((MysqlServerPacket) msg);
                            }
                        });
                    }
                });
                connectFuture = connectFuture.sync();
                if (!connectFuture.isSuccess()) {
                    throw new RuntimeException(connectFuture.cause());
                }
                channel = connectFuture.channel();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        MysqlServerPacket pollServer() {
            try {
                final MysqlServerPacket packet = serverPackets.poll(5, TimeUnit.SECONDS);
                if (packet == null) {
                    fail("链接超时.");
                }
                return packet;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public ChannelFuture write(MysqlClientPacket packet) {
            return channel.writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }

        public ChannelFuture query(String query) {
            channel.pipeline().replace(MysqlServerPacketDecoder.class, "decoder", new MysqlServerResultSetPacketDecoder());
            return write(new QueryCommand(0, query));
        }

        public void authenticate() {
            final Handshake handshake = (Handshake) pollServer();

            final HandshakeResponse response = HandshakeResponse
                    .create()
                    .addCapabilities(CLIENT_CAPABILITIES)
                    .username(user)
                    .addAuthData(MysqlNativePasswordUtil.hashPassword(password, handshake.getAuthPluginData()))
                    .database(database)
                    .authPluginName(Constants.MYSQL_NATIVE_PASSWORD)
                    .build();
            write(response);
        }

        @Override
        public void close() {
            try {
                channel.close().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}