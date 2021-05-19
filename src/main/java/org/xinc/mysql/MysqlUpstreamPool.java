package org.xinc.mysql;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.xinc.mysql.client.MysqlClient;
import org.xinc.mysql.client.MysqlClientProperty;

import java.util.Map;

@Slf4j
public class MysqlUpstreamPool extends BaseKeyedPooledObjectFactory<Map<String, Object>, MysqlClient> {

    @Override
    public MysqlClient create(Map<String, Object> stringObjectMap) throws Exception {
        log.info("获取客户端");
        return new MysqlClient(new MysqlClientProperty("/application-client.properties"),(Channel) stringObjectMap.get("downStream"));
    }

    @Override
    public PooledObject<MysqlClient> wrap(MysqlClient client) {
        return new DefaultPooledObject<>(client);
    }
}
