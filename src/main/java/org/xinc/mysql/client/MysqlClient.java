package org.xinc.mysql.client;

import ch.qos.logback.core.net.server.Client;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.xinc.mysql.codec.*;

import java.util.EnumSet;
import java.util.concurrent.*;

@Slf4j
public class MysqlClient {


    private EventLoopGroup eventLoopGroup;

    private Bootstrap bootstrap;

    MysqlClientProperty property = null;

    public Channel downstream;

    public Channel upstream;

    public MysqlClient() {

    }

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
            public void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(
                        new LoggingHandler(),
                        new ForwardHandler(downstream, mysqlClientProperty)
                );
            }
        });
        var cf = bootstrap.connect(property.server, property.port);

        cf.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                System.out.println("链接成功");
                upstream = cf.channel();
            }
        });
    }


}
