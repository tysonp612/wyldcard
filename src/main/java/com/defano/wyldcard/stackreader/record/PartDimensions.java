package com.defano.wyldcard.stackreader.record;

import com.defano.wyldcard.stackreader.misc.StackInputStream;

import java.awt.*;
import java.io.IOException;

public class PartDimensions {

    private short top;
    private short left;
    private short bottom;
    private short right;

    public static PartDimensions deserialize(StackInputStream sis) throws IOException {
        PartDimensions dimensions = new PartDimensions();
        dimensions.top = sis.readShort();
        dimensions.left = sis.readShort();
        dimensions.bottom = sis.readShort();
        dimensions.right = sis.readShort();
        return dimensions;
    }

    public Rectangle getPartRectangle() {
        return new Rectangle(left, top, right - left, bottom - top);
    }

    public short getTop() {
        return top;
    }

    public short getLeft() {
        return left;
    }

    public short getBottom() {
        return bottom;
    }

    public short getRight() {
        return right;
    }
}
