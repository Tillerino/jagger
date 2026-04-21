package org.tillerino.jagger.processor;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;

public class Main {
    public static void main(String[] args) throws JSQLParserException {
        Update parse = (Update) CCJSqlParserUtil.parse(
                "UPDATE blabla set dso.#updateColumns = (:dso.#updateValues) where dso.#keyColumns = :dso.#keyValues");
        parse.getUpdateSets().get(0).getColumns().add(new Column("fds"));
        System.out.println(parse);
        parse.accept(
                new StatementDeParser(
                        new ExpressionDeParser() {
                            @Override
                            public <S> StringBuilder visit(JdbcNamedParameter jdbcNamedParameter, S context) {
                                System.out.println(jdbcNamedParameter);
                                return null;
                            }

                            @Override
                            public <S> StringBuilder visit(Column tableColumn, S context) {
                                System.out.println(tableColumn);
                                return null;
                            }
                        },
                        new SelectDeParser(),
                        new StringBuilder()) {});
    }
}
