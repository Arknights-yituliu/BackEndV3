package com.lhs.entity.dto.item;



import java.util.List;


public class CompositeTableDTO {
    private String itemId;
    private String itemName;
    private Integer rarity;
    private Boolean resolve;
    private List<PathwayDTO> pathway;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getRarity() {
        return rarity;
    }

    public void setRarity(Integer rarity) {
        this.rarity = rarity;
    }

    public Boolean getResolve() {
        return resolve;
    }

    public void setResolve(Boolean resolve) {
        this.resolve = resolve;
    }

    public List<PathwayDTO> getPathway() {
        return pathway;
    }

    public void setPathway(List<PathwayDTO> pathway) {
        this.pathway = pathway;
    }
}



