package com.csgroupfive.texter.senders.util;

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

public class Emailer extends Sender {
    public Session session;

    private String user;

    public Emailer() {
        this("Emailer", SenderType.AMBIGUOUS);
    }
    public Emailer(String name, SenderType type) {
        super(name, type);

        // grab configs
        Dotenv dotenv = Dotenv.load();
        String host = dotenv.get("SMTP_SERVER");
        String port = dotenv.get("SMTP_PORT");
        String user = dotenv.get("SMTP_EMAIL");
        String pass = dotenv.get("SMTP_PASS");
        this.user = user;

        // config smtp
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        // create a session that we'll reuse
        // TODO: recreate session if it closes
        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
    }

    public ApiResponseStatus send_email(String message, String recipient) {
        return send_email("Email from Texter program", message, recipient);
    }

    public ApiResponseStatus send_email(String subject, String message, String recipient) {
        return send_email(subject, message, recipient, this.user);
    }

    public ApiResponseStatus send_email(String subject, String message, String recipient, String from) {
        MimeMessage msg = new MimeMessage(session);

        String to = recipient;
        try {
            // set from and to
            msg.setFrom(new InternetAddress(from));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            // I don't believe subject is usually displayed for email to sms, so just put something
            msg.setSubject(subject);
            // set the body of the email
            msg.setText(message); 

            // send the email
            Transport.send(msg);
            System.out.println("sent email to " + to);
        } catch (MessagingException e) {
            System.err.println("error building or sending message. likely to: is not valid: " + to);
            e.printStackTrace();
            return ApiResponseStatus.FAILED;
        }
        // return unknown if successful because email doesn't give us 
        // feedback that we can access within this timeframe of if it was successful or not
        return ApiResponseStatus.UNKNOWN;
    }
}
