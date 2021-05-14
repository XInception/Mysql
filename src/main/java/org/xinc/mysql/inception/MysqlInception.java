package org.xinc.mysql.inception;


import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.xinc.mysql.lexer.MySqlLexer;
import org.xinc.mysql.parser.MySqlParser;
import org.xinc.function.Inception;


@Slf4j
public class MysqlInception implements Inception {
    @Override
    public void checkRule(Object sql){
        log.info("check sql {}",sql);
        CharStream source = CharStreams.fromString(((String) sql).toUpperCase());
        MySqlLexer lexer = new MySqlLexer(source);
        MySqlParser parser = new MySqlParser(new CommonTokenStream(lexer));
        parser.setBuildParseTree(true);
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowingErrorListener());
        ParseTree tree = parser.root();
        MySqlParserVisitor visitor = new MySqlParserVisitor();
        visitor.visit(tree);
    }
}
