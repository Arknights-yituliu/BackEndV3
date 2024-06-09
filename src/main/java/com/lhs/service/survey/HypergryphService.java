package com.lhs.service.survey;

import com.lhs.entity.vo.survey.AKPlayerBindingListVO;

import java.util.HashMap;


public interface HypergryphService {


    AKPlayerBindingListVO getPlayerBindingsByHGToken(String token);

    AKPlayerBindingListVO getPlayerBindingsBySkland(HashMap<String, String> sklandCredAndToken);
}
