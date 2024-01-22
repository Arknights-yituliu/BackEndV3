package com.lhs.entity.vo.dev;

import lombok.Data;

@Data
public class DevelopLogVO {
    private String tag;
    private String author;
    private String text;
    private Long commitTime;

}
