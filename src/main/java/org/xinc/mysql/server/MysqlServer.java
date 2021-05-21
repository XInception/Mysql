package org.xinc.mysql.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.xinc.mysql.codec.MysqlClientConnectionPacketDecoder;
import org.xinc.mysql.codec.MysqlServerPacketEncoder;


@Slf4j
public class MysqlServer {

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);

    EventLoopGroup workerGroup = new NioEventLoopGroup();

    ChannelFuture f = null;

    MysqlServerProperty property = null;

    public MysqlServer(MysqlServerProperty mysqlServerProperty) {
        property = mysqlServerProperty;
        start(property);
    }

    public void start(MysqlServerProperty mysqlServerProperty) {
        property = mysqlServerProperty;
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
//                            pipeline.addLast(new LoggingHandler());
                            pipeline.addLast(new MysqlServerPacketEncoder());
                            pipeline.addLast(new MysqlClientConnectionPacketDecoder());
                            pipeline.addLast(new Mysql57ServerHandler());
                        }
                    });
            f = b.bind(property.server, property.port);
            log.info("mysql proxy server 启动完成 {} {} ", property.server, property.port);
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
