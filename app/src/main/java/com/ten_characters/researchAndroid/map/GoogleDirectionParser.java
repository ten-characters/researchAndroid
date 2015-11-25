package com.ten_characters.researchAndroid.map;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by austin on 7/07/15.
 */
public class GoogleDirectionParser {

    private static final String LOG_TAG = GoogleDirectionParser.class.getSimpleName();

    public List<List<HashMap<String, String>>> parseJSONDirections(JSONObject toParse) {
        List<List<HashMap<String, String>>> routeToReturn = new ArrayList<>();
        JSONArray routes, legs, steps;
        // Could later use this to show duration and distance of trip!
        // Let's think about using this either in the api or
        // client side to estimate
        // Either when adding it to the database or when shown
        JSONObject distance = null, duration = null;
        try {
            routes = toParse.getJSONArray("routes");

            for (int i = 0; i < routes.length(); i++) {
                legs = routes.getJSONObject(i).getJSONArray("legs");
                List<HashMap<String, String>> path = new ArrayList<>();
                
                // Now traverse each leg
                for (int j = 0; j < legs.length(); j++) {
                    // Distance
                    distance = legs.getJSONObject(j).getJSONObject("distance");
                    HashMap<String, String> hmDistance = new HashMap<>();
                    hmDistance.put("distance", distance.getString("text"));
                    
                    // Duration
                    duration = legs.getJSONObject(j).getJSONObject("duration");
                    HashMap<String, String> hmDuration = new HashMap<>();
                    hmDuration.put("duration", duration.getString("text"));

                    // Append distance and duration
                    path.add(hmDistance); path.add(hmDuration);

                    steps = legs.getJSONObject(j).getJSONArray("steps");

                    // ...Now travers the steps
                    for (int k = 0; k < steps.length(); k++) {
                        String polyline;
                        polyline = steps.getJSONObject(k).getJSONObject("polyline").getString("points");
                        List<LatLng> pointList = decodePolyline(polyline);

                        // ......Now traverse all points
                        for (int l = 0; l < pointList.size(); l++) {
                            HashMap<String, String> hm = new HashMap<>();
                            hm.put("lat", Double.toString(pointList.get(l).latitude));
                            hm.put("lng", Double.toString(pointList.get(l).longitude));
                            path.add(hm);
                        }
                    }
                }
                routeToReturn.add(path);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing google directions!", e);
        }
        return routeToReturn;
    }

    private List<LatLng> decodePolyline (String encodedLine) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encodedLine.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do
            {
                b = encodedLine.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do
            {
                b = encodedLine.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng(((lat / 1E5)), ((lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}
