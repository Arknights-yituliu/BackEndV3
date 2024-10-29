package com.lhs.service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogService {

    public void printLog(String message){
        log.info(message);
    }
}
