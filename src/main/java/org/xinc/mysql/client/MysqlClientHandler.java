package org.xinc.mysql.client;

import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.xinc.mysql.codec.*;

import static org.xinc.mysql.client.MysqlClient.CLIENT_CAPABILITIES;

@Slf4j
public class MysqlClientHandler extends ChannelInboundHandlerAdapter {


    Channel downStreamChanel = null;
    MysqlClientProperty property = null;

    public MysqlClientHandler(Channel downStreamChanel, MysqlClientProperty property) {
        this.downStreamChanel = downStreamChanel;
        this.property = property;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        System.out.println("数据包类型"+msg);
        if (msg instanceof Handshake) {
            CapabilityFlags.getCapabilitiexinctr(ctx.channel()).retainAll(((Handshake) msg).getCapabilities());
            log.info("开始认证");
            final HandshakeResponse response = HandshakeResponse
                    .create()
                    .addCapabilities(CLIENT_CAPABILITIES)
                    .username(property.user)
                    .addAuthData(MysqlNativePasswordUtil.hashPassword(property.password, ((Handshake) msg).getAuthPluginData()))
                    .database("test")
                    .authPluginName(Constants.MYSQL_NATIVE_PASSWORD)
                    .build();
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            System.out.println("服务器正在初始化请稍等");
            Thread.sleep(5000);
            System.out.println("服务器初始化完成");
            return;
        }
        System.out.println("mysql 服务器响应数据包");
        System.out.println(msg);
        if(msg instanceof  OkResponse){
           System.out.println("ok response 数据包"+msg);
        }
        if(downStreamChanel!=null){
            System.out.println("转发数据包");
            downStreamChanel.writeAndFlush(msg);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.print("发生异常: ");
        cause.printStackTrace(System.err);
        ctx.close();
    }
}
