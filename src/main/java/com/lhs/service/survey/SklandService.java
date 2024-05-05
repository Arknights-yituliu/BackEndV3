package com.lhs.service.survey;

import com.lhs.entity.dto.hypergryph.PlayerBinding;
import org.springframework.stereotype.Service;


public interface SklandService {


    PlayerBinding getPlayerBindings(String token);
}
