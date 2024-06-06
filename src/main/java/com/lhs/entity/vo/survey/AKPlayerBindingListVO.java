package com.lhs.entity.vo.survey;

import com.lhs.entity.dto.hypergryph.PlayerBinding;
import lombok.Data;

import java.util.List;

@Data
public class AKPlayerBindingListVO {
    private PlayerBinding playerBinding;
    private List<PlayerBinding> playerBindingList;
}
