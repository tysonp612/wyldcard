package com.defano.wyldcard.stackreader.record;

import com.defano.wyldcard.stackreader.enums.*;
import com.defano.wyldcard.stackreader.misc.StackInputStream;

import java.io.IOException;

public class PartProperties {

    private PartStyle style;
    private short titleWidthOrLastSelectedLine;
    private short iconIdOrFirstSelectedLine;
    private TextAlignment textAlign;
    private short textFontId;
    private short textSize;
    private FontStyle[] fontStyles;
    private short textHeight;
    private String name;
    private String script;
    private ExtendedPartFlag[] extendedFlags;

    public static PartProperties deserialize(StackInputStream sis, PartType partType, byte extendedFlagsMask) throws IOException {
        PartProperties properties = new PartProperties();
        properties.style = PartStyle.ofPartStyleId(sis.readByte());
        properties.titleWidthOrLastSelectedLine = sis.readShort();
        properties.iconIdOrFirstSelectedLine = sis.readShort();
        properties.textAlign = TextAlignment.fromAlignmentId(sis.readShort());
        properties.textFontId = sis.readShort();
        properties.textSize = sis.readShort();
        properties.fontStyles = FontStyle.fromBitmask(sis.readByte());
        sis.readByte();
        properties.textHeight = sis.readShort();
        properties.name = sis.readString();
        sis.readByte();
        properties.script = sis.readString();
        properties.extendedFlags = ExtendedPartFlag.fromBitmask(extendedFlagsMask);
        return properties;
    }

    public PartStyle getStyle() {
        return style;
    }

    public int getTitleWidth() {
        return titleWidthOrLastSelectedLine;
    }

    public int getLastSelectedLine() {
        return titleWidthOrLastSelectedLine;
    }

    public int getFirstSelectedLine() {
        return iconIdOrFirstSelectedLine;
    }

    public int getIconId() {
        return iconIdOrFirstSelectedLine;
    }

    public TextAlignment getTextAlign() {
        return textAlign;
    }

    public short getTextFontId() {
        return textFontId;
    }

    public short getTextSize() {
        return textSize;
    }

    public FontStyle[] getFontStyles() {
        return fontStyles;
    }

    public short getTextHeight() {
        return textHeight;
    }

    public String getName() {
        return name;
    }

    public String getScript() {
        return script;
    }

    public ExtendedPartFlag[] getExtendedFlags() {
        return extendedFlags;
    }
}
