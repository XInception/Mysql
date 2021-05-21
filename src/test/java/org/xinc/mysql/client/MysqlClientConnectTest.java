package org.xinc.mysql.client;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.*;

@Slf4j
class MysqlClientConnectTest{


    @Test
    void test() throws IOException, InterruptedException, ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");

        //3、获取数据库的连接对象
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/test?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8&useSSL=false", "root", "123456Zz..*");

        //4、定义sql语句
        String sql = "select version() as v;";

        //5、获取执行sql语句的对象
        Statement stat = con.createStatement();

        //6、执行sql并接收返回结果
        ResultSet res = stat.executeQuery(sql);

        while (res.next()){
           var v=res.getString("v");
           System.out.println(v);
        }
        //7、处理结果
        System.out.println(res);

        //8、释放资源
        stat.close();
        con.close();
    }

    @Test
    void test1() throws IOException, InterruptedException, ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");

        //3、获取数据库的连接对象
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost:13306/test?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8&useSSL=false", "root", "123456Zz..*");


        //4、定义sql语句
        String sql = "select version() as v;";

        //5、获取执行sql语句的对象
        Statement stat = con.createStatement();

        //6、执行sql并接收返回结果
        var res = stat.executeQuery(sql);
        System.out.println("查询结果");
        while (res.next()){
            var v=res.getString("v");
            System.out.println(v);
        }
        //8、释放资源
        stat.close();
        con.close();
    }

}