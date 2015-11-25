package com.ten_characters.researchAndroid.userInfo;

import static com.ten_characters.researchAndroid.server.ServerUtility.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * Created by austin on 7/22/15.
 */
public class Trailer implements Serializable {
    private String model, modelType, size, year, plate;
    public String photoPath;

    public Trailer(String model, String modelType, String size, String year,
                   String vin, String plate, String photoPath) {
        this.model = model;
        this.modelType = modelType;
        this.size = size;
        this.year = year;
        this.plate = plate;
        this.photoPath = photoPath;
    }

    public Trailer(JSONObject trailerJSON) throws JSONException {
        model = trailerJSON.getString(MODEL_KEY);
        modelType = trailerJSON.getString(MODEL_TYPE_KEY);
        size = trailerJSON.getString(EQUIP_SIZE_KEY);
        year = trailerJSON.getString(YEAR_KEY);
        plate = trailerJSON.getString(PLATE_KEY);
        photoPath = trailerJSON.getString(PHOTO_PATH_KEY);
    }

    public LinkedHashMap<String, String> getInfoMap() {
        LinkedHashMap<String, String> info = new LinkedHashMap<>(7);
        info.put(MODEL_KEY, model);
        info.put(MODEL_TYPE_KEY, modelType);
        info.put(EQUIP_SIZE_KEY, size);
        info.put(YEAR_KEY, year);
        info.put(PLATE_KEY, plate);
        info.put(PHOTO_PATH_KEY, photoPath);
        return info;
    }

}
