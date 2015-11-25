package com.ten_characters.researchAndroid.server;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by austin on 17/06/15.
 */
public class ServerRequestTask extends AsyncTask<String, Void, JSONObject> {

    private final String LOG_TAG = ServerRequestTask.class.getSimpleName();
    private static final String CONTENT_TYPE_PROP = "Content-Type";
    private static final String JSON_TEXT_TYPE = "application/json";

    private OnTaskCompleted listener;

    public ServerRequestTask(OnTaskCompleted listener) {
        this.listener = listener;
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        // Will either receive a [0] string uri  and a [1] string method alone with a [2] json data string
        String uri = params[0];
        String method = params[1];
        String data = null;
        // Pythonic methodology
        try {
            // Attach the Data as JSON Dictionary along in the body
            data = params[2];
        } catch (IndexOutOfBoundsException e) {}

        HttpURLConnection connection = null;
        BufferedReader reader = null;
        DataOutputStream os = null;
        InputStream is = null;
        JSONObject responseJSON = null;
        try {
            URL url = new URL(uri);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            //connection.setRequestProperty("Connection", "Keep-Alive");

            connection.setRequestProperty(CONTENT_TYPE_PROP, JSON_TEXT_TYPE);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            //connection.setDoOutput(true);

            // Send the request
            if (data != null) {
                os = new DataOutputStream(connection.getOutputStream());
                // Simply writes the JSON data in
                os.writeBytes(data);
                // This pushes everything out, ensuring that it gets sent
                os.flush();
            }

            // Now we shall get the response back
            // Will either be a token or a 403 response
            // Should be either in the form 'token': token
            // OR if there was an error, the response code

            int responseCode = -1;
            try {
                is = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer responseBuffer = new StringBuffer();
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    responseBuffer.append(line + "\n");
                }
                // Make JSON from the response
                responseJSON = new JSONObject(responseBuffer.toString());

            } catch (FileNotFoundException e) {
                responseCode = connection.getResponseCode();
                // Build the responseCode into a JSONObject to return
                return new JSONObject("{\"code\":\"" + responseCode + "\"}");
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem requesting server!", e);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing JSON!", e);
        } finally {
            // Must make sure all of these streams are closed if still open
            if(connection != null)
                connection.disconnect();
            try {
                os.close();
                is.close();
                reader.close();
            } catch (final IOException e) {
                Log.e(LOG_TAG, "Error closing stream", e);
            } catch (NullPointerException e) {}
        }
        // Try to return the JSON response
        return responseJSON;
    }



    @Override
    protected void onPostExecute(JSONObject result) {
        listener.onTaskCompleted(result);
    }
}