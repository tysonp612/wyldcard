package com.defano.hypertalk.ast.model;

import com.defano.hypertalk.ast.statement.StatementList;
import org.antlr.v4.runtime.ParserRuleContext;

public class NamedBlockParams {
    private final ParserRuleContext context;
    private final String onName;
    private final String endName;
    private final ParameterList parameters;
    private final StatementList body;

    private NamedBlockParams(Builder builder) {
        this.context = builder.context;
        this.onName = builder.onName;
        this.endName = builder.endName;
        this.parameters = builder.parameters;
        this.body = builder.body;
    }

    public static class Builder {
        private ParserRuleContext context;
        private String onName;
        private String endName;
        private ParameterList parameters = new ParameterList();
        private StatementList body;

        public Builder(ParserRuleContext context, String onName, String endName, StatementList body) {
            this.context = context;
            this.onName = onName;
            this.endName = endName;
            this.body = body;
        }

        public Builder parameters(ParameterList parameters) {
            this.parameters = parameters;
            return this;
        }

        public NamedBlockParams build() {
            return new NamedBlockParams(this);
        }
    }

    public ParserRuleContext getContext() {
        return context;
    }

    public String getOnName() {
        return onName;
    }

    public String getEndName() {
        return endName;
    }

    public ParameterList getParameters() {
        return parameters;
    }

    public StatementList getBody() {
        return body;
    }
}
