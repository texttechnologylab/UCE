package org.texttechnologylab.uce.common.models.corpus;

import org.texttechnologylab.uce.common.models.UIMAAnnotation;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="block")
/*
Not sure about this class... I don't think we need blocks for now.
 */
public class Block extends UIMAAnnotation {

    public Block(){
        super(-1, -1);
    }
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
