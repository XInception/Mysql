package org.xinc.mysql.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Admin
 */
public class MysqlClientProperty {

    String server;
    Integer port;
    String user;
    String password;
    String database;

    public MysqlClientProperty(String s) throws IOException {
        this.loadProperty(s);
    }

    public void loadProperty(InputStream stream) throws IOException {
        Properties properties = new Properties();
        properties.load(stream);
        this.server=properties.getProperty("app.api.server");
        this.port=Integer.parseInt(properties.getProperty("app.api.port"));
        this.user=properties.getProperty("app.api.user");
        this.password=properties.getProperty("app.api.password");
        this.database=properties.getProperty("app.api.database");
    }

    public void loadProperty(String path) throws IOException {
        loadProperty( this.getClass().getResourceAsStream(path));
    }
}
