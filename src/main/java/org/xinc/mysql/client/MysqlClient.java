package org.xinc.mysql.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.xinc.mysql.codec.*;

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

    public static void main(String[] argv) throws ClassNotFoundException, SQLException {
        var client=new MysqlClient();
        client.start();
    }

    private final BlockingQueue<MysqlServerPacket> serverPackets = new LinkedBlockingQueue<MysqlServerPacket>();

    public void start(){
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
        var cf=bootstrap.connect("127.0.0.1",13306);

        cf.addListener((ChannelFutureListener) future->{
            if(future.isSuccess()){
                future.channel().pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        log.info("读取数据");
                        if (msg instanceof Handshake) {
                            CapabilityFlags.getCapabilitiexinctr(ctx.channel()).retainAll(((Handshake) msg).getCapabilities());
                        }
                        serverPackets.add((MysqlServerPacket) msg);
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
        var c = cf.channel();
    }
}
