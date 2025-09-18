package com.csgroupfive.texter;


public interface Messagable {
    public ApiResponseStatus send_message(String message, String recipient);
}
