package com.csgroupfive.texter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.Builder;
import java.util.Hashtable;
import java.util.Map;

import org.json.JSONObject;

public class HttpApi {
    private HttpClient client;

    public HttpResponse<String> post(String url, JSONObject payload, Map<String, String> headers) throws IOException, InterruptedException {
        if (client == null) {
            client = HttpClient.newHttpClient();
        }

        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "application/json");
        }

        Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url));

        for (String hk : headers.keySet()) {
            builder = builder.header(hk, headers.get(hk));
        }

        HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }
    public HttpResponse<String> post(String url, JSONObject payload) throws IOException, InterruptedException {
        return post(url, payload, new Hashtable<String, String>());
    }
}
