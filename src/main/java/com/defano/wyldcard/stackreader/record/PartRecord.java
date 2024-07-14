package com.defano.wyldcard.stackreader.record;

import com.defano.wyldcard.stackreader.HyperCardStack;
import com.defano.wyldcard.stackreader.block.Block;
import com.defano.wyldcard.stackreader.enums.*;
import com.defano.wyldcard.stackreader.misc.ImportException;
import com.defano.wyldcard.stackreader.misc.StackInputStream;

import java.io.IOException;

public class PartRecord {

    private transient HyperCardStack stack;
    private short size;
    private short partId;
    private PartType partType;
    private PartFlag[] flags;
    private int family;
    private PartDimensions dimensions;
    private PartProperties properties;

    public static PartRecord deserialize(Block parent, short entrySize, byte[] data) throws ImportException {
        PartRecord part = new PartRecord();
        part.stack = parent.getStack();
        part.size = entrySize;

        try (StackInputStream sis = new StackInputStream(data)) {
            part.partId = sis.readShort();
            part.partType = PartType.fromTypeId(sis.readByte());
            part.flags = PartFlag.fromBitmask(sis.readByte());
            part.dimensions = PartDimensions.deserialize(sis);
            byte extendedFlagsMask = sis.readByte();
            part.family = extendedFlagsMask & 0x0f;
            part.properties = PartProperties.deserialize(sis, part.partType, extendedFlagsMask);
        } catch (IOException e) {
            throw new ImportException(parent, "Malformed part record.", e);
        }

        return part;
    }

    public PartDimensions getDimensions() {
        return dimensions;
    }

    public PartProperties getProperties() {
        return properties;
    }

    public short getSize() {
        return size;
    }

    public short getPartId() {
        return partId;
    }

    public PartType getPartType() {
        return partType;
    }

    public PartFlag[] getFlags() {
        return flags;
    }

    public int getFamily() {
        return family;
    }
}
