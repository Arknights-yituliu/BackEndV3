package com.lhs.entity.dto.survey;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QuestionnaireSubmitInfoDTO implements Serializable {

    private static  final Long SerializableUid = 1L;

    private Long id;
    private Integer questionnaireType;
    private List<String> operatorList;
}
