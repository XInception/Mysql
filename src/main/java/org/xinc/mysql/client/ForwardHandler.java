package org.xinc.mysql.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ForwardHandler extends ChannelInboundHandlerAdapter {
    Channel downStreamChanel = null;
    MysqlClientProperty property = null;

    public ForwardHandler(Channel downStreamChanel, MysqlClientProperty property) {
        this.downStreamChanel = downStreamChanel;
        this.property = property;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.info("转发数据包");
        downStreamChanel.writeAndFlush(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.info("发生异常: ");
        cause.printStackTrace(System.err);
        ctx.close();
    }
}
