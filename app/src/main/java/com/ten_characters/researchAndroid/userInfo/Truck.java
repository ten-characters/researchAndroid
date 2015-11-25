package com.ten_characters.researchAndroid.userInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.LinkedHashMap;

import static com.ten_characters.researchAndroid.server.ServerUtility.*;

/**
 * Created by austin on 7/22/15.
 */
public class Truck implements Serializable {
    private String model, year, plate;
    public String photoPath;

    public Truck(String model, String year, String vin, String plate, String photoPath) {
        this.model = model;
        this.year = year;
        this.plate = plate;
        this.photoPath = photoPath;
    }

    public Truck(JSONObject equipmentJson) throws JSONException {
        model = equipmentJson.getString(MODEL_KEY);
        year = equipmentJson.getString(YEAR_KEY);
        plate = equipmentJson.getString(PLATE_KEY);
        photoPath = equipmentJson.getString(PHOTO_PATH_KEY);
    }

    public LinkedHashMap<String, String> getInfoMap() {
        LinkedHashMap<String, String> info = new LinkedHashMap<>(5);
        info.put(MODEL_KEY, model);
        info.put(YEAR_KEY, year);
        info.put(PLATE_KEY, plate);
        info.put(PHOTO_PATH_KEY, photoPath);
        return info;
    }
}
