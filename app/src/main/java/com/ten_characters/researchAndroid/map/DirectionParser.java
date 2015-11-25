package com.ten_characters.researchAndroid.map;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by austin on 7/28/15.
 *
 * * So we don't have to parse on the main UI thread
 *
 *
 * It's like Bj's --> aka --> Costco for Canadians ! (?)
 *
 * */

public class DirectionParser {

    private final String LOG_TAG = DirectionParser.class.getSimpleName();

    private Context mContext;
    private GoogleMap mMap;
    private Polyline mTripLine;
    private Parser parser;

    public DirectionParser(Context context, GoogleMap map) {
        mContext = context;
        mMap = map;
    }

    public void parseInBackground(JSONObject jsonRoute) {
        if (mMap != null) {
            parser = new Parser();
            parser.execute(jsonRoute);
        }
    }

    public void stopAndRemove() {
        // Create stuff to stop
        if (parser != null) {
            parser.cancel(true);
        }
        if (mTripLine != null)
            mTripLine.remove();
    }

    private class Parser extends AsyncTask<JSONObject, Integer, List<List<HashMap<String, String>>> > {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(JSONObject... data) {
            JSONObject toParse;
            List<List<HashMap<String, String>>> routes;

            toParse = data[0];
            GoogleDirectionParser parser = new GoogleDirectionParser();
            routes = parser.parseJSONDirections(toParse);

            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions polylineOptions = null;

            if (result == null) {
                Toast.makeText(mContext, "Couldn't get directions from Google!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Now let us traverse the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();

                polylineOptions = new PolylineOptions();
                // Setup line options
                polylineOptions.width(10F);
                // Default is black
                //polylineOptions.color(R.color.accent_dark_purple);

                List<HashMap<String, String>> path = result.get(i);
                // Always returns two headers of distance and duration
                // at positions 0 and 1
                path.remove(0);
                path.remove(0);
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    LatLng position = new LatLng(
                            Double.parseDouble(point.get("lat")),
                            Double.parseDouble(point.get("lng"))
                    );

                    points.add(position);
                }

                // Add the points to the poly line
                polylineOptions.addAll(points);
            }

            // Now we add the poly line to the map!!
            if (mMap != null && polylineOptions != null)
                mTripLine = mMap.addPolyline(polylineOptions);
        }
    }
}

