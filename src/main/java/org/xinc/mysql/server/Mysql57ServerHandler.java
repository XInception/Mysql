package org.xinc.mysql.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.xinc.mysql.MysqlUpstreamPool;
import org.xinc.mysql.client.MysqlClient;
import org.xinc.mysql.codec.*;
import org.xinc.mysql.inception.MysqlInception;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
class Mysql57ServerHandler extends ChannelInboundHandlerAdapter {

    MysqlInception mysqlInception = new MysqlInception();


    private byte[] salt = new byte[20];

    KeyedObjectPool<Map<String, Object>, MysqlClient> upstreamPool = new GenericKeyedObjectPool<>(new MysqlUpstreamPool());

    HashMap<String, Object> config = new HashMap<>();

    public Mysql57ServerHandler() {
        new Random().nextBytes(salt);
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


        final EnumSet<CapabilityFlags> capabilities = CapabilityFlags.getImplicitCapabilities();
        CapabilityFlags.setCapabilitiexinctr(ctx.channel(), capabilities);
        //TODO 使用远程服务器的 服务器版本
        log.info("返回服务器的版本和服务器的能力");
        ctx.writeAndFlush(Handshake.builder()
                .serverVersion("0.0.1 XInception")
                .connectionId(1)
                .addAuthData(salt)
                .authPluginName(Constants.MYSQL_NATIVE_PASSWORD)
                .characterSet(MysqlCharacterSet.UTF8_BIN)
                .addCapabilities(capabilities)
                .build());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HandshakeResponse) {
            System.out.println("处理握手");
            handleHandshakeResponse(ctx, (HandshakeResponse) msg);
        } else{
            if(msg instanceof MysqlClientPacket ){
                System.out.println("转发消息给后端"+((MysqlClientPacket) msg).getSequenceId());
                mysqlInception.checkRule(msg);
                MysqlClient mysqlClient = (MysqlClient) ctx.channel().attr(AttributeKey.valueOf("mysql_connect")).get();
                mysqlClient.connect().thenAccept(m->{
                    m.write((MysqlClientPacket)msg);
                });
//                mysqlClient.write((MysqlClientPacket)msg);
            }else {
                System.out.println("未知的消息");
            }

        }

//        if (msg instanceof HandshakeResponse) {
//            System.out.println("处理握手");
//            handleHandshakeResponse(ctx, (HandshakeResponse) msg);
//        } else if (msg instanceof QueryCommand) {
//
//            try {
//                handleQuery(ctx, (QueryCommand) msg);
//            }catch (Exception e){
//                System.out.println("处理查询发生异常");
//                e.printStackTrace();
//            }
//
//        } else if (msg instanceof CommandPacket) {
//            System.out.println("处理命令");
//            handleCommond(ctx, (CommandPacket) msg);
//        } else {
//            System.out.println("收到消息" + msg);
//        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("发生异常");
        cause.printStackTrace();
        ctx.close();
    }

    private void handleCommond(ChannelHandlerContext ctx, CommandPacket cmd) {

        System.out.println(cmd.getCommand());
        if(cmd.getCommand()==Command.COM_INIT_DB){
            ctx.writeAndFlush(OkResponse.builder()
                    .sequenceId(cmd.getSequenceId() + 1)
                    .info("ok")
                    .sessionStateChanges("")
                    .addStatusFlags(
                            ServerStatusFlag.AUTO_COMMIT,
                            ServerStatusFlag.SESSION_STATE_CHANGED)
                    .build());
        }else{
            ctx.writeAndFlush(OkResponse.builder()
                    .sequenceId(cmd.getSequenceId() + 1)
                    .info("ok")
                    .sessionStateChanges("")
                    .addStatusFlags(
                            ServerStatusFlag.AUTO_COMMIT).build());
        }

    }

    private void handleHandshakeResponse(ChannelHandlerContext ctx, HandshakeResponse response) {
        log.info("处理握手请求 {} ", response.getUsername());
        log.info("处理握手attr {} ", response.getAttributes());
        log.info("处理握手attr {} ", response.getAuthPluginData());
        //TODO 验证用户名密码是否 正确
        //挑战随机数 界面对比
        ctx.pipeline().replace(MysqlClientPacketDecoder.class, "CommandPacketDecoder", new MysqlClientCommandPacketDecoder());
        log.info("发送确认包");
        ctx.writeAndFlush(OkResponse.builder()
                .sequenceId(response.getSequenceId() + 1)
                .addStatusFlags(
                        ServerStatusFlag.AUTO_COMMIT,
                        ServerStatusFlag.SESSION_STATE_CHANGED
                )
                .sessionStateChanges("ok")
                .info("ok")
                .build());
    }

    private void handleQuery(ChannelHandlerContext ctx, QueryCommand query) {
        final String queryString = query.getQuery();
        log.info("收到请求: {} {}", query.getCommand().name(), queryString);

        if (isServerSettingsQuery(queryString)) {
            sendSettingsResponse(ctx, query);
        } else if (isShowVar(queryString)){
            // TODO 处理请求
            log.info("查询变量");
            int sequenceId = query.getSequenceId();
            ctx.write(new ColumnCount(++sequenceId, 1));
            ctx.write(ColumnDefinition.builder()
                    .sequenceId(++sequenceId)
                    .catalog("catalog")
                    .schema("schema")
                    .table("table")
                    .orgTable("org_table")
                    .name("Variable_name")
                    .orgName("org_name")
                    .columnLength(10)
                    .type(ColumnType.MYSQL_TYPE_DOUBLE)
                    .addFlags(ColumnFlag.NUM)
                    .decimals(5)
                    .build());
            ctx.write(ColumnDefinition.builder()
                    .sequenceId(++sequenceId)
                    .catalog("catalog")
                    .schema("schema")
                    .table("table")
                    .orgTable("org_table")
                    .name("Value")
                    .orgName("org_name")
                    .columnLength(10)
                    .type(ColumnType.MYSQL_TYPE_DOUBLE)
                    .addFlags(ColumnFlag.NUM)
                    .decimals(5)
                    .build());
            ctx.write(new EofResponse(++sequenceId, 0));
            ctx.write(new ResultsetRow(++sequenceId, "test","value"));
            ctx.writeAndFlush(new EofResponse(++sequenceId, 0));
        } {
            log.info("默认查询");
            // TODO 处理请求  select version() as v;
            int sequenceId = query.getSequenceId();
            ctx.write(new ColumnCount(++sequenceId, 1));
            ctx.write(ColumnDefinition.builder()
                    .sequenceId(++sequenceId)
                    .catalog("catalog")
                    .schema("schema")
                    .table("table")
                    .orgTable("org_table")
                    .name("v")
                    .orgName("org_name")
                    .columnLength(10)
                    .type(ColumnType.MYSQL_TYPE_VARCHAR)
//                    .addFlags(ColumnFlag.)
                    .decimals(5)
                    .build());
            ctx.write(new EofResponse(++sequenceId, 0));
            ctx.write(new ResultsetRow(++sequenceId, "0.0.1"));
            ctx.writeAndFlush(new EofResponse(++sequenceId, 0));
        }
    }

    private boolean isServerSettingsQuery(String query) {
        query = query.toLowerCase();
        return query.contains("select") && !query.contains("from") && query.contains("@@");
    }

    private boolean isShowVar(String query) {
        query = query.toUpperCase();
        return query.contains("SHOW VARIABLES");
    }

    private static Pattern SETTINGS_PATTERN = Pattern.compile("@@(\\w+)\\sAS\\s(\\w+)");

    private void sendSettingsResponse(ChannelHandlerContext ctx, QueryCommand query) {
        final Matcher matcher = SETTINGS_PATTERN.matcher(query.getQuery());

        final List<String> values = new ArrayList<>();
        int sequenceId = query.getSequenceId();

        while (matcher.find()) {
            String systemVariable = matcher.group(1);
            String fieldName = matcher.group(2);
            switch (systemVariable) {
                case "character_set_client":
                case "character_set_connection":
                case "character_set_results":
                case "character_set_server":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_VAR_STRING, 12));
                    values.add("utf8");
                    break;
                case "collation_server":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_LONGLONG, 21));
                    values.add("utf8_general_ci");
                    break;
                case "init_connect":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_VAR_STRING, 0));
                    values.add("");
                    break;
                case "interactive_timeout":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_VAR_STRING, 21));
                    values.add("28800");
                    break;
                case "language":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_VAR_STRING, 0));
                    values.add("");
                    break;
                case "license":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_VAR_STRING, 21));
                    values.add("ASLv2");
                    break;
                case "lower_case_table_names":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_LONGLONG, 63));
                    values.add("2");
                    break;
                case "max_allowed_packet":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_LONGLONG, 63));
                    values.add("4194304");
                    break;
                case "net_buffer_length":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_LONGLONG, 63));
                    values.add("16384");
                    break;
                case "net_write_timeout":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_LONGLONG, 63));
                    values.add("60");
                    break;
                case "have_query_cache":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_LONGLONG, 6));
                    values.add("YES");
                    break;
                case "sql_mode":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_LONGLONG, 0));
                    values.add("");
                    break;
                case "system_time_zone":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_LONGLONG, 6));
                    values.add("UTC");
                    break;
                case "time_zone":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_LONGLONG, 12));
                    values.add("SYSTEM");
                    break;
                case "tx_isolation":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_LONGLONG, 12));
                    values.add("REPEATABLE-READ");
                    break;
                case "wait_timeout":
                    ctx.write(newColumnDefinition(sequenceId++, fieldName, systemVariable, ColumnType.MYSQL_TYPE_LONGLONG, 12));
                    values.add("28800");
                    break;
                default:
                    throw new Error("Unknown system variable " + systemVariable);
            }
        }
        ctx.write(new EofResponse(++sequenceId, 0));
        ctx.write(new ResultsetRow(++sequenceId, values.toArray(new String[values.size()])));
        ctx.writeAndFlush(new EofResponse(++sequenceId, 0));
    }

    private ColumnDefinition newColumnDefinition(int packetSequence, String name, String orgName, ColumnType columnType, int length) {
        return ColumnDefinition.builder()
                .sequenceId(packetSequence)
                .name(name)
                .orgName(orgName)
                .type(columnType)
                .columnLength(length)
                .build();
    }
}