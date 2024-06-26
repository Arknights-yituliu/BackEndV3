package com.lhs.service.survey;

import com.lhs.entity.vo.survey.AKPlayerBindingListVO;

import java.util.HashMap;
import java.util.Map;


public interface HypergryphService {


    AKPlayerBindingListVO getPlayerBindingsByHGToken(String token);

    Map<String,Object> getCredAndTokenAndPlayerBindingsByHgToken(String hgToken);

    AKPlayerBindingListVO getPlayerBindingsBySkland(HashMap<String, String> sklandCredAndToken);


}
