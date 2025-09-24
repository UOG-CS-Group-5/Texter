package com.csgroupfive.texter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.json.JSONArray;

public class StoreSingleton {
    private String dataFilePath;
    private JSONObject data;
    private static StoreSingleton instance;

    // this is private so that users can't create instances
    private StoreSingleton() {
        // path to data file
        String dir = System.getProperty("user.dir");
        String fn = "data.json";

        this.dataFilePath = dir + File.separator + fn;

        // load data from json file
        this.loadData();
    }

    public static StoreSingleton getInstance() {
        // create singleton if not yet exists
        if (instance == null) {
            instance = new StoreSingleton();
        }
        return instance;
    }

    private void loadData() {
        JSONObject json;
        try {
            // get data from file
            String content = new String(Files.readAllBytes(Paths.get(this.dataFilePath)));
            // convert it to java-usable json object
            json = new JSONObject(content);
        } catch (IOException e) {
            e.printStackTrace();
            // default to empy json
            json = new JSONObject();
        }
        // default values
        if (!json.has("recipients")) {
            json.put("recipients", new JSONArray());
        }
        if (!json.has("message")) {
            json.put("message", "");
        }
        if (!json.has("savedMessages")) {
            json.put("savedMessages", new JSONArray());
        }
        this.data = json;
    }

    private void saveData() {
        try {
            // write to file
            Files.write(
                Paths.get(this.dataFilePath), 
                // convert to string->bytes. pretty indent
                this.data.toString(2).getBytes()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getRecipients() {
        // get recipients as list of objects
        List<Object> recipientsList = this.data.getJSONArray("recipients").toList();
        // convert them to a list of strings
        List<String> ret = recipientsList.stream()
                                         .map(Object::toString)
                                         .collect(Collectors.toList());
        return ret;
    }

    public void setRecipients(List<String> recipients) {
        this.data.put("recipients", recipients);
        this.saveData();
    }

    public String getMessage() {
        return this.data.getString("message");
    }

    public void setMessage(String message) {
        this.data.put("message", message);
        this.saveData();
    }

    public List<String> getSavedMessages() {
        JSONArray arr = this.data.getJSONArray("savedMessages");
        return arr.toList().stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    public void addSavedMessage(String msg) {
        if (msg == null) return;
        String clean = msg.strip();
        if (clean.isEmpty()) return;
        this.data.getJSONArray("savedMessages").put(clean);
        this.saveData();
    }

    public void updateSavedMessage(int index, String newText) {
        if (newText == null) return;
        String clean = newText.strip();

        JSONArray arr = this.data.getJSONArray("savedMessages");
        if (index >= 0 && index < arr.length()) {
            arr.put(index, clean);     // write back into the JSON array
            this.saveData();           // persist to data.json
        }
    }

    public void updateAndMoveSavedMessageToFront(int index, String newText) {
        if (newText == null) return;
        String clean = newText.strip();
        if (clean.isEmpty()) return;

        org.json.JSONArray old = this.data.getJSONArray("savedMessages");
        if (index < 0 || index >= old.length()) return;

        org.json.JSONArray fresh = new org.json.JSONArray();
        fresh.put(clean); // put edited at front
        for (int i = 0; i < old.length(); i++) {
            if (i == index) continue;
            fresh.put(old.get(i));
        }
        this.data.put("savedMessages", fresh);
        this.saveData();
    }

    public void prependSavedMessage(String msg) {
        if (msg == null) return;
        String clean = msg.strip();
        if (clean.isEmpty()) return;

        org.json.JSONArray old = this.data.getJSONArray("savedMessages");
        org.json.JSONArray fresh = new org.json.JSONArray();
        fresh.put(clean); // new at front
        for (int i = 0; i < old.length(); i++) {
            fresh.put(old.get(i));
        }
        this.data.put("savedMessages", fresh);
        this.saveData();
    }

    public void removeSavedMessage(String msg) {
        if (msg == null) return;
        JSONArray arr = this.data.getJSONArray("savedMessages");
        // remove all matches of that exact text
        for (int i = arr.length() - 1; i >= 0; i--) {
            if (msg.equals(arr.getString(i))) {
                arr.remove(i);
            }
        }
        this.saveData();
    }

    public int getSavedMessagesCount() {
        return this.data.getJSONArray("savedMessages").length();
    }

    public void clearSavedMessages() {
        this.data.put("savedMessages", new JSONArray());
        this.saveData();
    }
}
