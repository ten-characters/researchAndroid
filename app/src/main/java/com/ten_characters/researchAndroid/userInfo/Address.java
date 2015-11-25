package com.ten_characters.researchAndroid.userInfo;

import com.ten_characters.researchAndroid.server.ServerUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by austin on 10/07/15.
 */
public class Address implements Serializable{
    private static final String LOG_TAG = Address.class.getSimpleName();

    public String address, city, state, country, zip;

    public Address(JSONObject jsonAddress) throws JSONException{
        address = jsonAddress.getString(ServerUtility.ADDRESS_KEY);
        city = jsonAddress.getString(ServerUtility.CITY_KEY);
        state = jsonAddress.getString(ServerUtility.STATE_KEY);
        country = jsonAddress.getString(ServerUtility.COUNTRY_KEY);
        zip = jsonAddress.getString(ServerUtility.ZIP_KEY);
    }

    public Address(String city, String state, String country, String zip) {
        this.address = "";
        this.city = city;
        this.state = state;
        this.country = country;
        this.zip = zip;
    }

    public String getCityAndZipFormat() {
        return city + "\n" + zip;
    }
    public String getDisplayFormat() {
        return address + "\n" + city + "," + state;
    }
    public String getOfferFormat() { return city + ", " + state + "\n" + zip; }


    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public String getZip() {
        return zip;
    }
}
