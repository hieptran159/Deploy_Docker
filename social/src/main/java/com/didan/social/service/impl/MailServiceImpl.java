package com.didan.social.service.impl;

import com.didan.social.service.MailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class MailServiceImpl implements MailService {
    private final Logger logger = LoggerFactory.getLogger(MailServiceImpl.class);
    private final Environment env;
    @Autowired
    public MailServiceImpl(Environment env){
        this.env = env;
    }
    @Override
    public void sendTextEmail(String email, String subject, String text) throws IOException {
        Email from = new Email();
        from.setEmail(env.getProperty("send_grid.from_email"));
        from.setName(env.getProperty("send_grid.from_name"));
        Email to = new Email(email);
        Content content = new Content("text/html", text);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(env.getProperty("send_grid.api_key"));
        Request request = new Request();
        try{
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            logger.info(response.getBody());
        }catch (IOException e){
            logger.error("Server Error");
            throw new IOException("Server Error", e);
        }
    }
}
