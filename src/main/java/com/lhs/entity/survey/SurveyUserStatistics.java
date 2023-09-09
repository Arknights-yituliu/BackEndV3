package com.lhs.entity.survey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class SurveyUserStatistics {

    @Id
    private Long id;

    private String uid;
    private int operatorCount;
}
