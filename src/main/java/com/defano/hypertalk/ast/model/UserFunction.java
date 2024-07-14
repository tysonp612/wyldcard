package com.defano.hypertalk.ast.model;

import com.defano.hypertalk.ast.statement.StatementList;
import org.antlr.v4.runtime.ParserRuleContext;

public class UserFunction extends NamedBlock {

    public UserFunction(ParserRuleContext context, String onName, String endName, ParameterList parameters, StatementList statements) {
        super(new NamedBlockParams.Builder(context, onName, endName, statements)
                .parameters(parameters)
                .build());
    }
}
