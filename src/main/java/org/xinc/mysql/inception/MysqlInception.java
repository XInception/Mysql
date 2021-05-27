package org.xinc.mysql.inception;


import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import org.xinc.function.Inception;
import org.xinc.function.InceptionException;
import org.xinc.mysql.CaseChangingCharStream;
import org.xinc.mysql.codec.CommandPacket;
import org.xinc.mysql.codec.QueryCommand;
import org.xinc.mysql.gen.MySqlLexer;
import org.xinc.mysql.gen.MySqlParser;


@Slf4j
public class MysqlInception implements Inception {
    @Override
    public void checkRule(Object sql) throws InceptionException {
        String queryString = "";
        if (sql instanceof QueryCommand) {
            queryString = ((QueryCommand) sql).getQuery();
        } else if (sql instanceof CommandPacket) {
            System.out.println("命令包:" + ((CommandPacket) sql).getCommand().name());
            return;
        } else {
            System.out.println("未知的数据包");
        }
        log.info("check sql {}", queryString);
//        CodePointCharStream cp =CharStreams.fromString(queryString);
//        CharStream source =new CaseChangingCharStream(cp,true);
//        MySqlLexer lexer = new MySqlLexer(source);
//        MySqlParser parser = new MySqlParser(new CommonTokenStream(lexer));
//        parser.setBuildParseTree(true);
//        parser.removeErrorListeners();
//        parser.addErrorListener(new ThrowingErrorListener());
//        ParseTree tree = parser.root();
//        MySqlParserVisitor visitor = new MySqlParserVisitor();
//        visitor.visit(tree);
    }
}
