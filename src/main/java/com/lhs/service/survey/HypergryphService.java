package com.lhs.service.survey;

import com.lhs.entity.vo.survey.AkPlayerBindingListVO;

import java.util.HashMap;
import java.util.Map;


public interface HypergryphService {


    AkPlayerBindingListVO getPlayerBindingsByHGToken(String token);

    Map<String,Object> getCredAndTokenAndPlayerBindingsByHgToken(String hgToken);

    AkPlayerBindingListVO getPlayerBindingsBySkland(HashMap<String, String> sklandCredAndToken);



}
