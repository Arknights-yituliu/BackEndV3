package com.lhs.entity.dto.drop;

import lombok.Data;

import java.util.List;

/**
 * MAA 回收站材料产出上传请求。
 */
@Data
public class RecycleDropDTO {
    private List<StageDropDetailDTO> drops;
    private String server;
    private String source;
    private String version;
}
