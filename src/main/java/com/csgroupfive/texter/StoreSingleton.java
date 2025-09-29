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

    public List<List<String>> getSavedMessages() {
        JSONArray arr = this.data.getJSONArray("savedMessages");

        // map list of list of objects to list of string arrays
        return arr.toList().stream()
                .map(o -> ((List<?>) o).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    public void addSavedMessage(String msg) {
        if (msg == null) return;
        String clean = msg.strip();
        if (clean.isEmpty()) return;
        this.data.getJSONArray("savedMessages").put(clean);
        this.saveData();
    }

    public void updateSavedMessage(int index, List<String> newText) {
        if (newText == null) return;
        List<String> clean = List.of(newText.get(0).strip(), newText.get(1).strip());
        if (String.join("", clean).isEmpty()) return;     // NEW: do not persist blanks

        JSONArray arr = this.data.getJSONArray("savedMessages");
        if (index >= 0 && index < arr.length()) {
            arr.put(index, clean);
            this.saveData();
        }
    }

    public void updateAndMoveSavedMessageToFront(int index, List<String> newText) {
        if (newText == null) return;
        List<String> clean = List.of(newText.get(0).strip(), newText.get(1).strip());
        if (String.join("", clean).isEmpty()) return;

        JSONArray old = this.data.getJSONArray("savedMessages");
        if (index < 0 || index >= old.length()) return;

        JSONArray fresh = new JSONArray();
        fresh.put(clean); // put edited at front
        for (int i = 0; i < old.length(); i++) {
            if (i == index) continue;
            fresh.put(old.get(i));
        }
        this.data.put("savedMessages", fresh);
        this.saveData();
    }

    public void prependSavedMessage(List<String> msg) {
        if (msg == null) return;
        List<String> clean = List.of(msg.get(0).strip(), msg.get(1).strip());
        if (String.join("", clean).isEmpty()) return;

        JSONArray old = this.data.getJSONArray("savedMessages");
        JSONArray fresh = new JSONArray();
        fresh.put(clean); // new at front
        for (int i = 0; i < old.length(); i++) {
            fresh.put(old.get(i));
        }
        this.data.put("savedMessages", fresh);
        this.saveData();
    }

    public void removeSavedMessage(int index) {
        JSONArray arr = this.data.getJSONArray("savedMessages");
        if (index >= 0 && index < arr.length()) {
            arr.remove(index);   // remove from JSON array
            this.saveData();     // persist
        }
    }

    public int getSavedMessagesCount() {
        return this.data.getJSONArray("savedMessages").length();
    }

    public void clearSavedMessages() {
        this.data.put("savedMessages", new JSONArray());
        this.saveData();
    }
}
