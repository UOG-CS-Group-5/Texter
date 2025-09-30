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
        List<Object> raw = arr.toList();
        return raw.stream().map(Object::toString).collect(Collectors.toList());
    }

    public void prependSavedMessage(String text) {
        String safe = text == null ? "" : text;
        List<String> list = new java.util.ArrayList<>(getSavedMessages());
        list.removeIf(s -> s.equals(safe));   // optional de-dup
        list.add(0, safe);
        this.data.put("savedMessages", new JSONArray(list));
        this.saveData();
    }

    public void updateSavedMessage(int index, String text) {
        List<String> list = new java.util.ArrayList<>(getSavedMessages());
        if (index < 0 || index >= list.size()) return;
        list.set(index, text == null ? "" : text);
        this.data.put("savedMessages", new JSONArray(list));
        this.saveData();
    }

    public void updateAndMoveSavedMessageToFront(int index, String text) {
        List<String> list = new java.util.ArrayList<>(getSavedMessages());
        if (index < 0 || index >= list.size()) return;
        String safe = text == null ? "" : text;
        list.remove(index);
        list.removeIf(s -> s.equals(safe));   // optional de-dup
        list.add(0, safe);
        this.data.put("savedMessages", new JSONArray(list));
        this.saveData();
    }

    public void removeSavedMessage(int index) {
        List<String> list = new java.util.ArrayList<>(getSavedMessages());
        if (index < 0 || index >= list.size()) return;
        list.remove(index);
        this.data.put("savedMessages", new JSONArray(list));
        this.saveData();
    }
}
