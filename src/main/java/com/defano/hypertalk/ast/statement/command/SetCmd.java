package com.defano.hypertalk.ast.statement.command;

import com.defano.wyldcard.runtime.ExecutionContext;
import com.defano.hypertalk.ast.model.specifier.PropertySpecifier;
import com.defano.hypertalk.ast.expression.Expression;
import com.defano.hypertalk.ast.statement.Command;
import com.defano.hypertalk.exception.HtException;
import org.antlr.v4.runtime.ParserRuleContext;

public class SetCmd extends Command {

    private final Expression expression;
    private final PropertySpecifier propertySpec;

    public SetCmd(ParserRuleContext context, PropertySpecifier propertySpec, Expression expression) {
        super(context, "set");
        this.propertySpec = propertySpec;
        this.expression = expression;
    }

    public void onExecute(ExecutionContext context) throws HtException {
        propertySpec.setProperty(context, expression);
    }
}
