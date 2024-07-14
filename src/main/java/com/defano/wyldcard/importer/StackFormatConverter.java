package com.defano.wyldcard.importer;

import com.defano.hypertalk.ast.model.Value;
import com.defano.wyldcard.part.bkgnd.BackgroundModel;
import com.defano.wyldcard.part.builder.*;
import com.defano.wyldcard.part.button.ButtonModel;
import com.defano.wyldcard.part.card.CardLayer;
import com.defano.wyldcard.part.card.CardModel;
import com.defano.wyldcard.part.field.FieldModel;
import com.defano.wyldcard.part.stack.StackModel;
import com.defano.wyldcard.runtime.ExecutionContext;
import com.defano.wyldcard.stackreader.HyperCardStack;
import com.defano.wyldcard.stackreader.block.*;
import com.defano.wyldcard.stackreader.enums.*;
import com.defano.wyldcard.stackreader.misc.ImportException;
import com.defano.wyldcard.stackreader.misc.UnsupportedVersionException;
import com.defano.wyldcard.stackreader.record.*;
import com.defano.wyldcard.thread.Invoke;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

public class StackFormatConverter {

    private final ConversionStatusObserver status;
    private final ConversionProgressObserver progress;

    private StackFormatConverter(ConversionStatusObserver status, ConversionProgressObserver progress) {
        this.status = status;
        this.progress = progress;
    }

    public static void convert(File stackFile, ConversionStatusObserver status, ConversionProgressObserver progress) {
        if (progress == null || status == null) {
            throw new IllegalArgumentException("Conversion observer cannot be null.");
        }

        if (stackFile != null) {
            StackFormatConverter importer = new StackFormatConverter(status, progress);
            Invoke.asynchronouslyOnWorkerThread(() -> importer.doConversion(stackFile));
        }
    }

    private void doConversion(File stackFile) {

        try {
            HyperCardStack hcStack = HyperCardStack.fromFile(stackFile);
            StackModel model = buildStack(new ExecutionContext(), stackFile.getName(), hcStack);
            status.onConversionSucceeded(model);

        } catch (FileNotFoundException e) {
            status.onConversionFailed("Cannot find or open the stack file " + stackFile.getAbsolutePath(), e);
        } catch (UnsupportedVersionException e) {
            status.onConversionFailed("WyldCard cannot import stacks from HyperCard 1.x.\nPlease use the \"Convert Stack...\" command in HyperCard 2.x to update this stack before importing it.", e);
        } catch (ImportException e) {
            status.onConversionFailed("File is not a HyperCard stack or the stack is corrupted.", e);
        } catch (Exception t) {
            status.onConversionFailed("An unexpected error occurred while reading the stack file.", t);
        }
    }

    private StackModel buildStack(ExecutionContext context, String name, HyperCardStack hcStack) {

        List<CardBlock> cardBlocks = hcStack.getCardBlocks();

        StackBlock stackBlock = hcStack.getBlock(StackBlock.class);
        StackModel stackModel = new StackModelBuilder()
                .withName(name)
                .withWidth(stackBlock.getWidth())
                .withHeight(stackBlock.getHeight())
                .withScript(stackBlock.getStackScript())
                .build();

        for (int cardIdx = 0; cardIdx < cardBlocks.size(); cardIdx++) {
            buildCard(context, cardBlocks.get(cardIdx), stackModel);
            progress.onConversionProgressUpdate(cardIdx + 1, cardBlocks.size(), "Importing card " + (cardIdx + 1) + " of " + cardBlocks.size() + ".");
        }

        return stackModel;
    }

    private void buildCard(ExecutionContext context, CardBlock cardBlock, StackModel stackModel) {

        CardModel cardModel = new CardModelBuilder(stackModel)
                .withId(cardBlock.getBlockId())
                .withBackgroundId(cardBlock.getBkgndId())
                .withName(cardBlock.getName())
                .withIsMarked(isCardMarked(cardBlock.getBlockId(), cardBlock.getStack()))
                .withCantDelete(Arrays.stream(cardBlock.getFlags()).anyMatch(f -> f == LayerFlag.CANT_DELETE))
                .withDontSearch(Arrays.stream(cardBlock.getFlags()).anyMatch(f -> f == LayerFlag.DONT_SEARCH))
                .withShowPict(Arrays.stream(cardBlock.getFlags()).noneMatch(f -> f == LayerFlag.HIDE_PICTURE))
                .withImage(cardBlock.getImage())
                .withScript(cardBlock.getScript())
                .build();

        buildBackground(cardBlock.getBkgndBlock(), stackModel);

        buildParts(cardBlock.getParts(), cardModel, cardBlock);

        for (PartContentRecord pcr : cardBlock.getContents()) {
            applyUnsharedButtonHilite(context, pcr, cardModel);
            applyTextContents(context, pcr, cardModel, false);
            applyTextStyles(context, pcr, cardModel, cardBlock, false);
        }

        for (PartContentRecord pcr : cardBlock.getBkgndBlock().getContents()) {
            applyTextContents(context, pcr, cardModel, true);
            applyTextStyles(context, pcr, cardModel, cardBlock, true);
        }

        stackModel.addCard(cardModel);
    }

    private void applyTextStyles(ExecutionContext context, PartContentRecord pcr, CardModel cardModel, CardLayerBlock cardBlock, boolean sharedText) {
        FieldModel field;

        if (pcr.isBackgroundPart()) {
            field = cardModel.getBackgroundModel().getFieldModels().stream()
                    .filter(f -> f.getId() == pcr.getRawPartId())
                    .findFirst()
                    .orElse(null);
        } else {
            field = cardModel.getFieldModels().stream()
                    .filter(f -> f.getId() == -pcr.getRawPartId())
                    .findFirst()
                    .orElse(null);
        }

        if (field != null && (!sharedText || field.get(context, FieldModel.PROP_SHAREDTEXT).booleanValue())) {
            int cardId = cardModel.getId();

            if (pcr.isPlaintext()) {
                field.applyFont(context, cardId, 0, field.get(context, FieldModel.PROP_TEXTFONT).toString());
                field.applyFontSize(context, cardId, 0, field.get(context, FieldModel.PROP_TEXTSIZE).integerValue());
                field.applyFontStyle(context, cardId, 0, field.get(context, FieldModel.PROP_TEXTSTYLE));
            } else {
                applyStyleSpans(context, field, cardModel.getId(), cardBlock.getStack(), pcr.getStyleSpans());
            }
        }
    }

    private void applyTextContents(ExecutionContext context, PartContentRecord pcr, CardModel cardModel, boolean sharedText) {
        if (pcr.isBackgroundPart()) {
            FieldModel field = cardModel.getBackgroundModel().getField(pcr.getRawPartId());

            if (field != null && (!sharedText || field.get(context, FieldModel.PROP_SHAREDTEXT).booleanValue())) {
                field.setCurrentCardId(cardModel.getId());
                field.set(context, FieldModel.PROP_TEXT, new Value(pcr.getText()));
            }
        }
    }

    private void applyUnsharedButtonHilite(ExecutionContext context, PartContentRecord pcr, CardModel cardModel) {
        if (pcr.isBackgroundPart()) {
            ButtonModel bm = cardModel.getBackgroundModel().getButton(pcr.getRawPartId());

            if (bm != null) {
                bm.setCurrentCardId(cardModel.getId());
                bm.set(context, ButtonModel.ALIAS_HILITE, new Value(pcr.isBkgndButtonHilited()));
            }
        }
    }

    private void buildBackground(BackgroundBlock backgroundBlock, StackModel stackModel) {
        int backgroundId = backgroundBlock.getBlockId();

        if (stackModel.getBackground(backgroundId) != null) {
            return;
        }

        BackgroundModel backgroundModel = new BackgroundModelBuilder(stackModel)
                .withName(backgroundBlock.getName())
                .withId(backgroundId)
                .withCantDelete(Arrays.stream(backgroundBlock.getFlags()).anyMatch(f -> f == LayerFlag.CANT_DELETE))
                .withDontSearch(Arrays.stream(backgroundBlock.getFlags()).anyMatch(f -> f == LayerFlag.DONT_SEARCH))
                .withShowPict(Arrays.stream(backgroundBlock.getFlags()).noneMatch(f -> f == LayerFlag.HIDE_PICTURE))
                .withImage(backgroundBlock.getImage())
                .withScript(backgroundBlock.getScript())
                .build();

        buildParts(backgroundBlock.getParts(), backgroundModel, backgroundBlock);

        stackModel.addBackground(backgroundModel);
    }

    private void buildParts(PartRecord[] parts, CardLayer parent, CardLayerBlock block) {
        for (int partNumber = 0; partNumber < parts.length; partNumber++) {
            PartRecord partRecord = parts[partNumber];
            PartDimensions dimensions = partRecord.getDimensions();
            PartProperties properties = partRecord.getProperties();

            if (partRecord.getPartType() == PartType.BUTTON) {
                buildButton(partRecord, dimensions, properties, partNumber, parent, block);
            } else {
                buildField(partRecord, dimensions, properties, partNumber, parent, block);
            }
        }
    }

    private void buildButton(PartRecord partRecord, PartDimensions dimensions, PartProperties properties, int partNumber, CardLayer parent, CardLayerBlock block) {

        ButtonModel buttonModel = new ButtonModelBuilder(parent.getType().asOwner(), parent.getParentPartModel())
                .withPartNumber(partNumber)
                .withTop(dimensions.getTop())
                .withLeft(dimensions.getLeft())
                .withWidth(dimensions.getRight() - dimensions.getLeft())
                .withHeight(dimensions.getBottom() - dimensions.getTop())
                .withName(properties.getName())
                .withId(partRecord.getPartId())
                .withPartStyle(properties.getStyle().hypertalkName())
                .withFamily(partRecord.getFamily())
                .withTextSize(properties.getTextSize())
                .withTextFont(block.getStack().getBlock(FontTableBlock.class).getFont(properties.getTextFontId()).getFontName())
                .withTextStyle(FontStyle.asHypertalkList(properties.getFontStyles()))
                .withTextAlign(properties.getTextAlign().name())
                .withIconId(properties.getIconId())
                .withScript(properties.getScript())
                .withContents(block.getPartContents(partRecord.getPartId()).getText())
                .withSelectedItem(properties.getFirstSelectedLine())
                .withShowName(Arrays.stream(properties.getExtendedFlags()).anyMatch(f -> f == ExtendedPartFlag.SHOW_NAME))
                .withIsEnabled(Arrays.stream(partRecord.getFlags()).noneMatch(f -> f == PartFlag.DISABLED))
                .withAutoHilite(Arrays.stream(properties.getExtendedFlags()).anyMatch(f -> f == ExtendedPartFlag.AUTO_HILITE))
                .withHilite(Arrays.stream(properties.getExtendedFlags()).anyMatch(f -> f == ExtendedPartFlag.HILITE))
                .withSharedHilite(Arrays.stream(properties.getExtendedFlags()).noneMatch(f -> f == ExtendedPartFlag.NO_SHARING_HILITE))
                .withIsVisible(Arrays.stream(partRecord.getFlags()).noneMatch(f -> f == PartFlag.HIDDEN))
                .build();

        parent.addPartModel(buttonModel);
    }

    private void buildField(PartRecord partRecord, PartDimensions dimensions, PartProperties properties, int partNumber, CardLayer parent, CardLayerBlock block) {

        FieldModel fieldModel = new FieldModelBuilder(parent.getType().asOwner(), parent.getParentPartModel())
                .withPartNumber(partNumber)
                .withTop(dimensions.getTop())
                .withLeft(dimensions.getLeft())
                .withWidth(dimensions.getRight() - dimensions.getLeft())
                .withHeight(dimensions.getBottom() - dimensions.getTop())
                .withName(properties.getName())
                .withId(partRecord.getPartId())
                .withPartStyle(properties.getStyle().name())
                .withIsVisible(Arrays.stream(partRecord.getFlags()).noneMatch(f -> f == PartFlag.HIDDEN))
                .withDontWrap(Arrays.stream(partRecord.getFlags()).anyMatch(f -> f == PartFlag.DONT_WRAP))
                .withDontSearch(Arrays.stream(partRecord.getFlags()).anyMatch(f -> f == PartFlag.DONT_SEARCH))
                .withSharedText(Arrays.stream(partRecord.getFlags()).anyMatch(f -> f == PartFlag.SHARED_TEXT))
                .withTextSize(properties.getTextSize())
                .withTextFont(block.getStack().getBlock(FontTableBlock.class).getFont(properties.getTextFontId()).getFontName())
                .withTextStyle(FontStyle.asHypertalkList(properties.getFontStyles()))
                .withTextAlign(properties.getTextAlign().name())
                .withText(block.getPartContents(partRecord.getPartId()).getText())
                .withAutoTab(Arrays.stream(partRecord.getFlags()).anyMatch(f -> f == PartFlag.AUTO_TAB))
                .withLockText(Arrays.stream(partRecord.getFlags()).anyMatch(f -> f == PartFlag.LOCK_TEXT))
                .withAutoSelect(Arrays.stream(properties.getExtendedFlags()).anyMatch(f -> f == ExtendedPartFlag.AUTO_SELECT))
                .withShowLines(Arrays.stream(properties.getExtendedFlags()).anyMatch(f -> f == ExtendedPartFlag.SHOW_LINES))
                .withWideMargins(Arrays.stream(properties.getExtendedFlags()).anyMatch(f -> f == ExtendedPartFlag.WIDE_MARGINS))
                .withMultipleLines(Arrays.stream(properties.getExtendedFlags()).anyMatch(f -> f == ExtendedPartFlag.MULTIPLE_LINES))
                .withScript(properties.getScript())
                .build();

        parent.addPartModel(fieldModel);
    }

    private void applyStyleSpans(ExecutionContext context, FieldModel fieldModel, int cardId, HyperCardStack stack, StyleSpanRecord[] styleSpans) {

        FontTableBlock fontTableBlock = stack.getBlock(FontTableBlock.class);
        StyleTableBlock styleTableBlock = stack.getBlock(StyleTableBlock.class);

        for (StyleSpanRecord record : styleSpans) {
            StyleRecord style = styleTableBlock.getStyle(record.getStyleId());
            int position = record.getTextPosition();

            if (style.getFontId() != -1) {
                fieldModel.applyFont(context, cardId, position, fontTableBlock.getFont(style.getFontId()).getFontName());
            } else {
                fieldModel.applyFont(context, cardId, position, fieldModel.get(context, FieldModel.PROP_TEXTFONT).toString());
            }

            if (style.getFontSize() != -1) {
                fieldModel.applyFontSize(context, cardId, position, (int) style.getFontSize());
            } else {
                fieldModel.applyFontSize(context, cardId, position, fieldModel.get(context, FieldModel.PROP_TEXTSIZE).integerValue());
            }

            String styleString = FontStyle.asHypertalkList(style.getStyles());
            fieldModel.applyFontStyle(context, cardId, position, new Value(styleString));
        }
    }

    private boolean isCardMarked(int cardId, HyperCardStack stack) {
        for (PageBlock thisPage : stack.getBlocks(PageBlock.class)) {
            for (PageEntryRecord thisEntry : thisPage.getPageEntries()) {
                if (thisEntry.getCardId() == cardId) {
                    return Arrays.stream(thisEntry.getFlags()).anyMatch(f -> f == PageFlag.MARKED_CARD);
                }
            }
        }

        return false;
    }

}
