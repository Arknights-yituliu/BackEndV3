package com.lhs.entity.dto.rougeSeed;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Data
public class RougeSeedDTO {
    private Long seedId;
    private Long seed;
    private Long uid;
    private Integer rating;
    private String rougeVersion;
    private String rougeTheme;
    private String squad;
    private String operatorTeam;
    private String description;
    private List<String> tags;
    private String summaryImageLink;

}
