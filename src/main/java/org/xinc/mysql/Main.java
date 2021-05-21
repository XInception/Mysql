package org.xinc.mysql;



import org.xinc.mysql.server.MysqlServer;
import org.xinc.mysql.server.MysqlServerProperty;

import java.io.IOException;

public class Main {

    private static MysqlServer server;

    public static void main(String[] args) {
        server = new MysqlServer();
        try {
            server.start(new MysqlServerProperty("/application-server.properties"));
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
