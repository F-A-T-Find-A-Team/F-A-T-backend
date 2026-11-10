package com.F_A_T.F_A_T.domain.auth.service;

import com.F_A_T.F_A_T.global.exception.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSender {
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    public SmtpEmailSender(ObjectProvider<JavaMailSender> sender,@Value("${app.mail.from:}") String from) { this.sender=sender;this.from=from; }
    @Override public void sendCode(String email,String code) {
        var transport=sender.getIfAvailable();
        if (transport==null || from.isBlank()) throw new ApiException(ErrorCode.MAIL_DELIVERY_FAILED);
        var message=new SimpleMailMessage();message.setFrom(from);message.setTo(email);
        message.setSubject("F.A.T. 이메일 인증");message.setText("인증 코드: "+code+"\n5분 이내에 입력해 주세요.");
        try { transport.send(message); }
        catch (org.springframework.mail.MailException ex) { throw new ApiException(ErrorCode.MAIL_DELIVERY_FAILED); }
    }
}
