package com.csgroupfive.texter.senders.util;

public class Sender implements Messagable {
    public String name = "Sender";
    public SenderType type = SenderType.AMBIGUOUS;

    public Sender(String name, SenderType type) {
        // senders have a name and type to help debug and differentiate
        // between them when iterating through a list of them
        this.name = name;
        this.type = type;
    }
    public Sender() {
    }

    public String getName() {
        return this.name;
    }
    public SenderType getType() {
        return this.type;
    }
    // this is unimplemented on this base class. it should be implemented on child classes
    public ApiResponseStatus send_message(String message, String recipient) {
        throw new UnsupportedOperationException("This class should be extended, not used on its own");
    }
}
