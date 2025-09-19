package com.csgroupfive.texter.senders.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.Builder;
import java.util.Hashtable;
import java.util.Map;

import org.json.JSONObject;

public class HttpApi implements Named {
    private HttpClient client;
    public String name = "HttpApi";

    public String getName() {
        return name;
    }

    public HttpResponse<String> post(String url, JSONObject payload, Map<String, String> headers) throws IOException, InterruptedException {
        // set client for reuse if that helps at all
        if (client == null) {
            client = HttpClient.newHttpClient();
        }

        // set application/json Content-Type by default
        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "application/json");
        }

        // start building the post request starting with the url
        Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url));

        // add the headers
        for (String hk : headers.keySet()) {
            builder = builder.header(hk, headers.get(hk));
        }

        // finish with a post request with the payload
        HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();
        
        // make the request and return the result
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }

    // overload with no additional headers
    public HttpResponse<String> post(String url, JSONObject payload) throws IOException, InterruptedException {
        return post(url, payload, new Hashtable<String, String>());
    }
}
