package com.example.wisdomclassroomgroup.entity;

import org.json.JSONException;
import org.json.JSONObject;


public class HandShakeBean extends DefaultSendBean {

    public HandShakeBean() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("cmd", 54);
            jsonObject.put("handshake", "Hello the OkSocket");
            content = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}
