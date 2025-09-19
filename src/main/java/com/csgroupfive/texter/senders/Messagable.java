package com.csgroupfive.texter.senders;

public interface Messagable {
    public ApiResponseStatus send_message(String message, String recipient);
}
