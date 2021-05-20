package org.xinc.mysql.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.xinc.mysql.codec.*;
import org.xinc.mysql.server.MysqlServerProperty;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MysqlClient {

    protected static final EnumSet<CapabilityFlags> CLIENT_CAPABILITIES = CapabilityFlags.getImplicitCapabilities();

    static {
        CLIENT_CAPABILITIES.addAll(EnumSet.of(CapabilityFlags.CLIENT_PLUGIN_AUTH, CapabilityFlags.CLIENT_SECURE_CONNECTION, CapabilityFlags.CLIENT_CONNECT_WITH_DB));
    }

    private EventLoopGroup eventLoopGroup;

    private Bootstrap bootstrap;

    MysqlClientProperty property = null;

    private final BlockingQueue<MysqlServerPacket> serverPackets = new LinkedBlockingQueue<MysqlServerPacket>();

    Channel downstream;

    Channel c;

    public MysqlClient(MysqlClientProperty mysqlClientProperty, Channel downStream) {
        downstream = downStream;
        property = mysqlClientProperty;
        start(mysqlClientProperty, downStream);
    }

    public void start(MysqlClientProperty mysqlClientProperty, Channel channel) {
        downstream = channel;
        property = mysqlClientProperty;
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                CapabilityFlags.setCapabilitiexinctr(ch, CLIENT_CAPABILITIES);
                ch.pipeline().addLast(new LoggingHandler());
                ch.pipeline().addLast(new MysqlServerConnectionPacketDecoder());
                ch.pipeline().addLast(new MysqlClientPacketEncoder());
            }
        });
        var cf = bootstrap.connect(property.server, property.port);

        cf.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                future.channel().pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

                        if (msg instanceof Handshake) {
                            CapabilityFlags.getCapabilitiexinctr(ctx.channel()).retainAll(((Handshake) msg).getCapabilities());
                            log.info("开始认证");
                            final HandshakeResponse response = HandshakeResponse
                                    .create()
                                    .addCapabilities(CLIENT_CAPABILITIES)
                                    .username(property.user )
                                    .addAuthData(MysqlNativePasswordUtil.hashPassword(property.password, ((Handshake)msg).getAuthPluginData()))
                                    .database("test")
                                    .authPluginName(Constants.MYSQL_NATIVE_PASSWORD)
                                    .build();
                            ctx.writeAndFlush(response).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                        }

                        if(msg instanceof  ResultsetRow) {
                            System.out.println("-------\n");
                            ((ResultsetRow)msg).getValues().stream().forEach(s->{
                                System.out.println(s);
                            });
                        }
                    }
                });
            }
        });
        try {
            cf.sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!cf.isSuccess()) {
            throw new RuntimeException(cf.cause());
        }
        c = cf.channel();
    }

    ChannelFuture  query(String query){
        c.pipeline().replace(MysqlServerPacketDecoder.class, "decoder", new MysqlServerResultSetPacketDecoder());
        return write(new QueryCommand(0, query));
    }

    public ChannelFuture write(MysqlClientPacket packet) {
        return c.writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}
