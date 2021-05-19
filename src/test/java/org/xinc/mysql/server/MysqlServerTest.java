package org.xinc.mysql.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MysqlServerTest {

    @Test
    void beforeAll() throws IOException {
        MysqlServer mysqlServer=new MysqlServer(new MysqlServerProperty("/application-server.properties"));
    }
}