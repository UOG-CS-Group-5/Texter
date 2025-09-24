package com.csgroupfive.texter.senders;

import com.csgroupfive.texter.senders.util.ApiResponseStatus;
import com.csgroupfive.texter.senders.util.Emailer;
import com.csgroupfive.texter.senders.util.SenderType;


public class EmailToSmsSender extends Emailer {
    public String numberPostfix;
    public String emailToSmsDomain;

    public EmailToSmsSender(String numberPostfix, String emailToSmsDomain) {
        super("Email to SMS: " + emailToSmsDomain, SenderType.SMS);
        // some email to sms services have a letter or so after the number in the email address
        this.numberPostfix = numberPostfix;
        this.emailToSmsDomain = emailToSmsDomain;
    }

    public ApiResponseStatus send_message(String message, String recipient) {
        String from = "no-reply@nodomain.com";
        // build address of format: 6711231234@sms.gta.net
        String to = recipient + numberPostfix + "@" + emailToSmsDomain;
        
        return this.send_email("Email to SMS", message, to, from);
    }
}
