package org.hongxi.cloud.sample.api;

import java.io.Serializable;

public class EchoRequest implements Serializable {

    private String message;

    public EchoRequest() {
    }

    public EchoRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
