package com.defano.hypertalk.ast.model;

import com.defano.hypertalk.ast.statement.Statement;
import com.defano.hypertalk.ast.statement.StatementList;
import com.defano.hypertalk.exception.HtSyntaxException;
import com.defano.hypertalk.exception.HtUncheckedSemanticException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Collection;

public class NamedBlock {

    public final String name;
    public final StatementList statements;
    public final ParameterList parameters;
    public final ParserRuleContext context;

    public static NamedBlock anonymousBlock(StatementList statementList) {
        return new NamedBlock(new NamedBlockParams.Builder(null, "", "", statementList).build());
    }

    public NamedBlock(NamedBlockParams params) {
        if (params.getOnName() == null) {
            throw new HtUncheckedSemanticException(new HtSyntaxException("Missing 'on' clause in handler definition.", params.getContext().getStart()));
        }

        if (params.getEndName() == null) {
            throw new HtUncheckedSemanticException(new HtSyntaxException("Missing 'end' clause in handler definition.", params.getContext().getStart()));
        }

        if (!params.getOnName().equalsIgnoreCase(params.getEndName())) {
            throw new HtUncheckedSemanticException(new HtSyntaxException("Found 'end " + params.getEndName() + "' but expected 'end " + params.getOnName() + "'.", params.getContext().getStart()));
        }

        this.name = params.getOnName();
        this.statements = params.getBody();
        this.parameters = params.getParameters();
        this.context = params.getContext();
    }

    public Collection<Statement> findStatementsOnLine(int line) {
        return statements.findStatementsOnLine(line);
    }

    public Integer getLineNumber() {
        if (context != null && context.getStart() != null) {
            return context.getStart().getLine();
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
