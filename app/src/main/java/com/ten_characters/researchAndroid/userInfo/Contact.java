package com.ten_characters.researchAndroid.userInfo;

import com.ten_characters.researchAndroid.server.ServerUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by austin on 7/29/15.
 */
public class Contact implements Serializable {
    public String email, name, phone;

    public Contact(JSONObject jsonContact) throws JSONException {
        email = jsonContact.getString(ServerUtility.EMAIL_KEY);
        name = jsonContact.getString(ServerUtility.NAME_KEY);
        phone = jsonContact.getString(ServerUtility.PHONE_KEY);
    }

    // Just tryna out new ways to store stuff in sqlite
    public Contact(String commaDelimitedString) {
        int lastComma = 0;
        for (int i = 0; i < commaDelimitedString.length(); i++) {
            if (commaDelimitedString.charAt(i) == ',') {
                if (email == null) {
                    email = commaDelimitedString.substring(lastComma, i);
                }
                else if (name == null) {
                    name = commaDelimitedString.substring(lastComma, i);
                    phone = commaDelimitedString.substring(i+1, commaDelimitedString.length());
                    break;
                }
                lastComma = i + 1;
            }
        }
    }

    public String toCommaDelimitedString() {
        return email + "," + name + "," + phone;
    }
}
