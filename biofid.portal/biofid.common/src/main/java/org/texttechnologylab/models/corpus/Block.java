package org.texttechnologylab.models.corpus;

import org.texttechnologylab.models.UIMAAnnotation;

/*
Not sure about this class... I don't think we need blocks for now.
 */
public class Block extends UIMAAnnotation {

    private String blockType;

    public Block(int begin, int end) {
        super(begin, end);
    }

    public String getBlockType() {
        return blockType;
    }
    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }
}
