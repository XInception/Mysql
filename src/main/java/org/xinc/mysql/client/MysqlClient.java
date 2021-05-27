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

    boolean isConnect = false;

    public boolean isConnect() {
        return isConnect;
    }


    protected static final EnumSet<CapabilityFlags> CLIENT_CAPABILITIES = CapabilityFlags.getImplicitCapabilities();

    static {
        CLIENT_CAPABILITIES.addAll(
                EnumSet.of(CapabilityFlags.CLIENT_PLUGIN_AUTH,
                        CapabilityFlags.CLIENT_SECURE_CONNECTION,
                        CapabilityFlags.CLIENT_CONNECT_WITH_DB
                ));
    }

    private EventLoopGroup eventLoopGroup;

    private Bootstrap bootstrap;

    MysqlClientProperty property = null;

    Channel downstream;

    Channel c;

    public MysqlClient() {

    }

    public MysqlClient(MysqlClientProperty mysqlClientProperty, Channel downStream) {
        downstream = downStream;
        property = mysqlClientProperty;
        start(mysqlClientProperty, downStream);
    }

    public  CompletableFuture<MysqlClient> connect(){
        return CompletableFuture.supplyAsync(() -> this.auth());
    }

    BlockingQueue blockingQueue=new ArrayBlockingQueue(1);

    public MysqlClient auth()  {
        if(isConnect()){
            return this;
        }
        Object msg= null;
        try {
            msg = blockingQueue.poll(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("队列读取异常");
            e.printStackTrace();
        }

        System.out.println("发送认证数据包");

        final HandshakeResponse response = HandshakeResponse
                .create()
                .addCapabilities(CLIENT_CAPABILITIES)
                .username(property.user)
                .addAuthData(MysqlNativePasswordUtil.hashPassword(property.password, ((Handshake) msg).getAuthPluginData()))
                .database(property.database)
                .addAttribute("client_name","xince")
                .authPluginName(Constants.MYSQL_NATIVE_PASSWORD)
                .build();

        c.writeAndFlush(response).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

        try {
            msg = blockingQueue.poll(5, TimeUnit.SECONDS);
            System.out.println("认证成功");
            System.out.println("修改解码包");
            c.pipeline().replace(MysqlServerPacketDecoder.class, "decoder", new MysqlServerResultSetPacketDecoder());
            this.isConnect=true;
        } catch (InterruptedException e) {
            System.out.println("等到OK 回复异常");
            e.printStackTrace();
        }
        return this;
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
                CapabilityFlags.setCapabilitiexinctr(ch, CLIENT_CAPABILITIES);
                ch.pipeline().addLast(
                        new LoggingHandler(),
                        new MysqlServerConnectionPacketDecoder(),
                        new MysqlClientPacketEncoder()
                        );
            }
        });
        var cf = bootstrap.connect(property.server, property.port);

        cf.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                future.channel().pipeline().addLast(
                        new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        System.out.println("消息 id"+((MysqlPacket)msg).getSequenceId());
                        System.out.println(msg);

                        if (msg instanceof Handshake) {
                            System.out.println("开始握手");
                            CapabilityFlags.getCapabilitiexinctr(ctx.channel()).retainAll(((Handshake) msg).getCapabilities());
                            blockingQueue.add(msg);
                            return;
                        }
                        if (msg instanceof OkResponse || msg instanceof ErrorResponse) {
                            System.out.println("OK / ERROR 数据包\n");
                            blockingQueue.add(msg);
                        }
//
                        if (msg instanceof ResultsetRow) {
                            System.out.println("结果开始 ==================================");
                            ((ResultsetRow) msg).getValues().forEach(System.out::println);

                            System.out.println("结果结束 ==================================");
                        }
//

                        if (downstream != null) {
                            System.out.println("下游写入数据");
                            downstream.write(msg);
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

    CompletableFuture<MysqlClient> query(String query) {
//        c.pipeline().replace(MysqlServerPacketDecoder.class, "decoder", new MysqlServerResultSetPacketDecoder());
        return CompletableFuture.supplyAsync(() -> write(new QueryCommand(0, query)));
//        return write(new QueryCommand(0, query));
    }

    public MysqlClient write(MysqlClientPacket packet) {

        Integer sid= (int)c.attr(AttributeKey.valueOf("sequenceId")).get();
        System.out.println("重写消息id"+sid);
//        ((MysqlClientPacket)packet).setSequenceId(sid+1);
        c.writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        Object ret = null;
        try {
            ret = blockingQueue.poll(5, TimeUnit.SECONDS);
            System.out.println("读取数据");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(ret);
         return  this;
    }


}
