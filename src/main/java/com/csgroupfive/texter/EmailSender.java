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
        // some email to sms services have a letter or so after the number in the email address
        this.numberPostfix = numberPostfix;
        this.emailToSmsDomain = emailToSmsDomain;

        // grab configs
        Dotenv dotenv = Dotenv.load();
        String host = dotenv.get("SMTP_SERVER");
        String port = dotenv.get("SMTP_PORT");
        String user = dotenv.get("SMTP_EMAIL");
        String pass = dotenv.get("SMTP_PASS");

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

    public String getName() {
        return name;
    }

    public ApiResponseStatus send_message(String message, String recipient) {
        MimeMessage msg = new MimeMessage(session);
        // it seems that with some SMTP servers, the smtp email is used instead of this from address
        String from = "no-reply@nodomain.com";
        // build address of format: 6711231234@sms.gta.net
        String to = recipient + numberPostfix + "@" + emailToSmsDomain;
        try {
            // set from and to
            msg.setFrom(new InternetAddress(from));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            // I don't believe subject is usually displayed for email to sms, so just put something
            msg.setSubject("Email To SMS");
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
        // return unknown if successful because email doesn't give us any feedback if the text message was sent
        return ApiResponseStatus.UNKNOWN;
    }
}
