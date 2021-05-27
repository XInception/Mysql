package org.xinc.mysql.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.xinc.function.InceptionException;
import org.xinc.mysql.Feature;
import org.xinc.mysql.MysqlUpstreamPool;
import org.xinc.mysql.PacketUtils;
import org.xinc.mysql.client.MysqlClient;
import org.xinc.mysql.codec.*;
import org.xinc.mysql.inception.MysqlInception;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
class Mysql57ServerForwardHandler extends ChannelInboundHandlerAdapter {

    MysqlInception mysqlInception = new MysqlInception();

    KeyedObjectPool<Map<String, Object>, MysqlClient> upstreamPool = new GenericKeyedObjectPool<>(new MysqlUpstreamPool());

    HashMap<String, Object> config = new HashMap<>();

    MysqlServerProperty property;

    public Mysql57ServerForwardHandler(MysqlServerProperty property) {
        this.property=property;
        System.out.println(Feature.RECORD_REQUEST_RESPONSE_LOG.name());
        log.info("测试 {}" ,property.getProperty(Feature.RECORD_REQUEST_RESPONSE_LOG.name(),Feature.RECORD_REQUEST_RESPONSE_LOG.getDefaultVal()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端已经离线 返还 mysql 句柄");
        MysqlClient mysqlClient = (MysqlClient) ctx.channel().attr(AttributeKey.valueOf("mysql_connect")).get();

        upstreamPool.returnObject(config, mysqlClient);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端已经上线 获取mysql 句柄");
        config.put("downStream",ctx.channel());
        MysqlClient mysqlClient = upstreamPool.borrowObject(config);
        ctx.channel().attr(AttributeKey.valueOf("mysql_connect")).set(mysqlClient);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MysqlClient mysqlClient = (MysqlClient) ctx.channel().attr(AttributeKey.valueOf("mysql_connect")).get();
        try {
            log.info("执行mysql inception");
            mysqlInception.checkRule(PacketUtils.toMysqlPacket(msg));
        } catch (InceptionException e) {
            e.printStackTrace();
            ctx.writeAndFlush(new ErrorResponse(0, 0, new byte[]{0x01}, e.getMessage()).toByteBuf());
            return;
        }
        mysqlClient.upstream.writeAndFlush(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("发生异常");
        cause.printStackTrace();
        ctx.close();
    }
}