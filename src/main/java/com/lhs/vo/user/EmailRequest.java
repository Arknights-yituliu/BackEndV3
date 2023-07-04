package com.lhs.vo.user;

import lombok.Data;

@Data
public class EmailRequest {
    private String  From;//发送者
    private String  To;//收件人
    private String  Subject;//邮件主题
    private String  Text;//邮件内容

}
