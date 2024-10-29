package com.lhs.entity.dto.rougeSeed;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Data
public class RougeSeedDTO {
    private Long seedId;
    private String seed;
    private Long uid;
    private String rougeVersion;
    private String rougeTheme;
    private String squad;
    private String operatorTeam;
    private String description;
    private List<String> tags;
    private String summaryImageLink;

}
