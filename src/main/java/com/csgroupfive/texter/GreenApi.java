package com.csgroupfive.texter;

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

        API_TOKEN = dotenv.get("GREEN_API_TOKEN");
        ID_INSTANCE = dotenv.get("GREEN_ID_INSTANCE");
    }

    @Override
    public String getName() {
        return name;
    }

    public ApiResponseStatus send_message(String message, String recipient) {
        String url = "https://7105.api.greenapi.com/waInstance" + ID_INSTANCE + "/sendMessage/" + API_TOKEN;

        JSONObject payload = new JSONObject();
        payload.put("chatId", recipient + "@c.us");
        payload.put("message", message);

        try {
            HttpResponse<String> response = post(url, payload);
            System.err.println(response.body());
            if (response.statusCode() != 200){
                return ApiResponseStatus.FAILED;
            }
        } catch (IOException | InterruptedException e) {
            return ApiResponseStatus.FAILED;
        }
        return ApiResponseStatus.UNKNOWN;
    }
}
