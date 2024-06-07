package com.lhs.service.util;


import com.lhs.entity.dto.util.EmailFormDTO;

public interface Email163Service {


    void sendSimpleEmail(EmailFormDTO email);

    Integer CreateVerificationCode(String emailAddress, Integer maxCodeNum);

    Boolean compareVerificationCode(String inputCode, String emailAddress);
}
