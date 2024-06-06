package com.lhs.service.survey;

import com.lhs.entity.dto.hypergryph.PlayerBinding;
import com.lhs.entity.vo.survey.AKPlayerBindingListVO;
import org.springframework.stereotype.Service;

import java.util.Map;


public interface SklandService {


    AKPlayerBindingListVO getPlayerBindings(String token);
}
