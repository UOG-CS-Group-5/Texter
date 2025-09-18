package com.csgroupfive.texter;

import java.io.IOException;

public interface Messagable {
    public ApiResponseStatus send_message(String message, String recipient) throws IOException, InterruptedException;
}
