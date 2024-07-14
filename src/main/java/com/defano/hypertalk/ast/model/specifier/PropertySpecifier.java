package com.defano.hypertalk.ast.model.specifier;

import com.defano.hypertalk.ast.model.enums.LengthAdjective;
import com.defano.hypertalk.ast.model.enums.Preposition;
import com.defano.wyldcard.WyldCard;
import com.defano.wyldcard.part.model.PartModel;
import com.defano.hypertalk.ast.expression.Expression;
import com.defano.hypertalk.ast.expression.container.MenuItemExp;
import com.defano.hypertalk.ast.expression.container.PartExp;
import com.defano.hypertalk.ast.model.chunk.Chunk;
import com.defano.hypertalk.delegate.MenuPropertiesDelegate;
import com.defano.hypertalk.delegate.ChunkPropertiesDelegate;
import com.defano.hypertalk.exception.HtException;
import com.defano.hypertalk.exception.HtSemanticException;
import com.defano.wyldcard.runtime.ExecutionContext;

public class PropertySpecifier {

    private final String property;
    private final Expression partExp;
    private final LengthAdjective lengthAdjective;

    public PropertySpecifier(String globalProperty) {
        this(globalProperty, null);
    }

    public PropertySpecifier(String property, Expression part) {
        this(LengthAdjective.DEFAULT, property, part);
    }

    public PropertySpecifier(LengthAdjective lengthAdjective, String property, Expression part) {
        this.property = property;
        this.partExp = part;
        this.lengthAdjective = lengthAdjective;
    }

    public boolean isGlobalPropertySpecifier(ExecutionContext context) {
        return partExp == null && getMenuItem(context) == null;
    }

    public boolean isMenuItemPropertySpecifier(ExecutionContext context) {
        return getMenuItem(context) != null;
    }

    public boolean isChunkPropertySpecifier(ExecutionContext context) {
        return getChunk(context) != null;
    }

    public LengthAdjective getLengthAdjective() {
        return lengthAdjective;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(ExecutionContext context, Expression expression) throws HtException {
        if (isGlobalPropertySpecifier(context)) {
            WyldCard.getInstance().getWyldCardPart().trySet(context, getProperty(), expression.evaluate(context));
        } else if (isMenuItemPropertySpecifier(context)) {
            MenuPropertiesDelegate.setProperty(context, getProperty(), expression.evaluate(context), getMenuItem(context));
        } else if (isChunkPropertySpecifier(context)) {
            ChunkPropertiesDelegate.setProperty(context, getProperty(), expression.evaluate(context), getChunk(context), getPartExp(context).evaluateAsSpecifier(context));
        } else {
            context.setProperty(getProperty(), getPartExp(context).evaluateAsSpecifier(context), Preposition.INTO, null, expression.evaluate(context));
        }
    }

    public String getAdjectiveAppliedPropertyName(ExecutionContext context) {
        PartModel model = getPartModel(context);

        if (model != null && model.isAdjectiveSupportedProperty(property)) {
            if (lengthAdjective == LengthAdjective.DEFAULT) {
                return model.getDefaultAdjectiveForProperty(property).apply(property);
            } else {
                return lengthAdjective.apply(property);
            }
        } else {
            return property;
        }
    }

    public Chunk getChunk(ExecutionContext context) {
        if (partExp == null) {
            return null;
        } else {
            PartExp factor = partExp.factor(context, PartExp.class);
            return factor == null ? null : factor.getChunk();
        }
    }

    public PartModel getPartModel(ExecutionContext context) {
        if (partExp == null) {
            return null;
        } else {
            return partExp.partFactor(context, PartModel.class);
        }
    }

    public PartExp getPartExp(ExecutionContext context) throws HtException {
        if (partExp == null) {
            return null;
        } else {
            return partExp.factor(context, PartExp.class, new HtSemanticException("Expected a part here."));
        }
    }

    public MenuItemSpecifier getMenuItem(ExecutionContext context) {
        if (partExp == null) {
            return null;
        } else {
            MenuItemExp factor = partExp.factor(context, MenuItemExp.class);
            return factor == null ? null : factor.item;
        }
    }
}
