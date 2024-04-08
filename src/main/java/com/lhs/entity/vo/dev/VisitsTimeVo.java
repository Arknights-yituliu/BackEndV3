package com.lhs.entity.vo.dev;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;


import java.util.Date;

@Data
public class VisitsTimeVo {

    private Long startTime;

    private Long endTime;
}
