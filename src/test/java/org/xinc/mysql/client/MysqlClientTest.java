package org.xinc.mysql.client;

import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

class MysqlClientTest {

    public static MysqlClient mysqlClient;

    @BeforeAll
    static void beforeAll() throws IOException {
        mysqlClient=new MysqlClient(new MysqlClientProperty ("/application-client.properties"),null);


    }

}