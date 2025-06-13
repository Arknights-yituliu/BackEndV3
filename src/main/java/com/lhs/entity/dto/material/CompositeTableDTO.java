package com.lhs.entity.dto.material;

import java.util.List;


public class CompositeTableDTO {
    private String id;
    private String name;
    private Boolean resolve;
    private List<ItemCostDTO> pathway;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getResolve() {
        return resolve;
    }

    public void setResolve(Boolean resolve) {
        this.resolve = resolve;
    }

    public List<ItemCostDTO> getPathway() {
        return pathway;
    }

    public void setPathway(List<ItemCostDTO> pathway) {
        this.pathway = pathway;
    }
}



