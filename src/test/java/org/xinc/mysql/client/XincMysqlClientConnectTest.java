package org.xinc.mysql.client;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
class XincMysqlClientConnectTest extends MysqlClientTest {
    @Test
    void test1() throws IOException {
//        mysqlClient.query("select version() as v;");

        mysqlClient.connect().thenAccept(s->{
            s.query("select * from test").thenAccept(r->{});
            s.query("select version() as v;").thenAccept(r->{});
        });

        System.in.read();
    }


    @Test
    void  test2() throws IOException, ExecutionException, InterruptedException {
//        CompletableFuture<String> completableFuture
//                = CompletableFuture.supplyAsync(() -> "Hello")
//                        .thenApply((s)->s+"world");
//
//        CompletableFuture<Void> future = completableFuture
//                .thenAccept(s -> System.out.println("Computation returned: " + s));
//
//       System.out.println( future.get());
    }
}