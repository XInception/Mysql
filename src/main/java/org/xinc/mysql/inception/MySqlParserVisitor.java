package org.xinc.mysql.inception;


import lombok.extern.slf4j.Slf4j;



@Slf4j
public class MySqlParserVisitor extends org.xinc.mysql.parser.MySqlParserBaseVisitor<Object> {

    @Override
    public Object visitDmlStatement(org.xinc.mysql.parser.MySqlParser.DmlStatementContext ctx) {
        return super.visitDmlStatement(ctx);
    }

    @Override
    public Object visitDdlStatement(org.xinc.mysql.parser.MySqlParser.DdlStatementContext ctx) {
        return super.visitDdlStatement(ctx);
    }
}
