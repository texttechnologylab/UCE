package org.texttechnologylab.models.dto;

import java.util.List;

public class LayeredSearchLayerDto {

    private int depth;
    private List<LayeredSearchSlotDto> slots;
    private boolean isDirty;
    private String slotsHash;

    public LayeredSearchLayerDto() {
    }

    public void calculateSlotsHash(){
        if(this.slots == null || this.slots.isEmpty()) {
            this.slotsHash = "";
            return;
        }

        StringBuilder fullString = new StringBuilder();
        for(var slot: this.slots){
            fullString.append(slot.getValue());
        }
        this.slotsHash = String.valueOf(fullString.toString().hashCode());
    }

    public String getSlotsHash() {
        return slotsHash;
    }

    public void setSlotsHash(String slotsHash) {
        this.slotsHash = slotsHash;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public List<LayeredSearchSlotDto> getSlots() {
        return slots;
    }

    public void setSlots(List<LayeredSearchSlotDto> slots) {
        this.slots = slots;
    }
}
