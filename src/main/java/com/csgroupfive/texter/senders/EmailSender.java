package com.csgroupfive.texter.senders;

import com.csgroupfive.texter.senders.util.ApiResponseStatus;
import com.csgroupfive.texter.senders.util.Emailer;
import com.csgroupfive.texter.senders.util.SenderType;

public class EmailSender extends Emailer {
    public EmailSender() {
        super("Email", SenderType.EMAIL);
    }

    public ApiResponseStatus send_message(String message, String recipient) {
        return this.send_email(message, recipient);
    }
}
