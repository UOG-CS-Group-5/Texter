package com.csgroupfive.texter;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import io.github.cdimascio.dotenv.Dotenv;

public class EmailSender implements Messagable, Named {
    public String numberPostfix;
    public String emailToSmsDomain;
    public Session session;

    public String name = "Email to SMS";

    public EmailSender(String numberPostfix, String emailToSmsDomain) {
        this.name = name + ": " + emailToSmsDomain;
        this.numberPostfix = numberPostfix;
        this.emailToSmsDomain = emailToSmsDomain;

        Dotenv dotenv = Dotenv.load();
        String host = dotenv.get("SMTP_SERVER");
        String port = dotenv.get("SMTP_PORT");
        String user = dotenv.get("SMTP_EMAIL");
        String pass = dotenv.get("SMTP_PASS");

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
    }

    public String getName() {
        return name;
    }

    public ApiResponseStatus send_message(String message, String recipient) {
        MimeMessage msg = new MimeMessage(session);
        String from = "no-reply@nodomain.com";
        String to = recipient + numberPostfix + "@" + emailToSmsDomain;
        try {
            msg.setFrom(new InternetAddress(from));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            msg.setSubject("Email To SMS");
            msg.setText(message); 

            Transport.send(msg);
            System.out.println("sent email to " + to);
        } catch (MessagingException e) {
            System.err.println("error building or sending message. likely to: is not valid: " + to);
            e.printStackTrace();
            return ApiResponseStatus.FAILED;
        }
        return ApiResponseStatus.UNKNOWN;
    }
}
