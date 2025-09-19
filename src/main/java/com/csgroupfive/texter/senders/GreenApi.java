package com.csgroupfive.texter.senders;

import java.io.IOException;
import java.net.http.HttpResponse;

import org.json.JSONObject;

import io.github.cdimascio.dotenv.Dotenv;

public class GreenApi extends HttpApi implements Messagable {
    private String API_TOKEN;
    private String ID_INSTANCE;
    public String name = "GreenAPI";

    public GreenApi() {
        Dotenv dotenv = Dotenv.load();

        // grab configs
        API_TOKEN = dotenv.get("GREEN_API_TOKEN");
        ID_INSTANCE = dotenv.get("GREEN_ID_INSTANCE");
    }

    @Override
    public String getName() {
        return name;
    }

    public ApiResponseStatus send_message(String message, String recipient) {
        String url = "https://7105.api.greenapi.com/waInstance" + ID_INSTANCE + "/sendMessage/" + API_TOKEN;

        // build payload {"chatId": "1231231234@c.us", "message": "blah"}
        JSONObject payload = new JSONObject();
        payload.put("chatId", recipient + "@c.us");
        payload.put("message", message);

        try {
            // make a post (see HttpApi.java)
            HttpResponse<String> response = post(url, payload);
            System.err.println(response.body());
            // 200 is success. if anything other than success, return failed
            if (response.statusCode() != 200){
                return ApiResponseStatus.FAILED;
            }
        } catch (IOException | InterruptedException e) {
            return ApiResponseStatus.FAILED;
        }
        // GreenAPI is inconsistant so we don't know if it successfully sent a message even if the post was successful
        return ApiResponseStatus.UNKNOWN;
    }
}
