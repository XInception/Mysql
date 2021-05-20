package org.xinc.mysql.client;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Slf4j
class MysqlClientConnectTest extends  MysqlClientTest{


    @Test
    void test1() throws IOException, InterruptedException {
        log.info("更新");
        Thread.sleep(10000);
        mysqlClient.query("show databases");
        Thread.sleep(10000);

    }

}