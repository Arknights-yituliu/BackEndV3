package com.lhs.entity.dto.user;

import com.lhs.entity.dto.material.StageConfigDTO;
import lombok.Data;

@Data
public class UserConfigDTO {
    private Long configId;
    private StageConfigDTO stageConfig;
}
