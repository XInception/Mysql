package org.xinc.mysql.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Admin
 */
public class MysqlServerProperty extends Properties {

    String server;
    Integer port;

    public MysqlServerProperty(String s) throws IOException {
        this.loadProperty(s);
    }

    public void loadProperty(InputStream stream) throws IOException {
        load(stream);
        this.server=this.getProperty("app.api.server");
        this.port=Integer.parseInt(this.getProperty("app.api.port"));
    }

    public void loadProperty(String path) throws IOException {
        loadProperty( this.getClass().getResourceAsStream(path));
    }
}
