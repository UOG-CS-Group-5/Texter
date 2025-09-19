package com.csgroupfive.texter.senders.util;

public interface Messagable {
    public ApiResponseStatus send_message(String message, String recipient);
}
