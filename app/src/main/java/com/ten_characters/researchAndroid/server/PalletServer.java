package com.ten_characters.researchAndroid.server;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.ten_characters.researchAndroid.DocumentUtility;
import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.GlobalApp;
import com.ten_characters.researchAndroid.activities.AuthActivity;
import com.ten_characters.researchAndroid.auth.AccountUtility;
import com.ten_characters.researchAndroid.auth.ServerAuthenticate;
import com.ten_characters.researchAndroid.userInfo.Shipment;
import com.ten_characters.researchAndroid.userInfo.User;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import static com.ten_characters.researchAndroid.auth.AccountUtility.getToken;
import static com.ten_characters.researchAndroid.auth.AccountUtility.saveToken;
import static com.ten_characters.researchAndroid.server.ServerUtility.ADD_PAYMENT_EXT;
import static com.ten_characters.researchAndroid.server.ServerUtility.API_VERSION_1;
import static com.ten_characters.researchAndroid.server.ServerUtility.API_VERSION_1_1;
import static com.ten_characters.researchAndroid.server.ServerUtility.AUTH_TOKEN_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.BASE_API_URL;
import static com.ten_characters.researchAndroid.server.ServerUtility.DRIVERS_EXT;
import static com.ten_characters.researchAndroid.server.ServerUtility.DRIVER_ID_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.EMAIL_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.GET_MINE_EXT;
import static com.ten_characters.researchAndroid.server.ServerUtility.LOGIN_EXT;
import static com.ten_characters.researchAndroid.server.ServerUtility.MAX_RETURNED_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.ONLY_CURRENT_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.PASSWORD_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.RANGE_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.RESPOND_EXT;
import static com.ten_characters.researchAndroid.server.ServerUtility.SHIPMENTS_EXT;
import static com.ten_characters.researchAndroid.server.ServerUtility.SHIPMENT_ID_KEY;
import static com.ten_characters.researchAndroid.server.ServerUtility.TOG_ACTIVE_EXT;
import static com.ten_characters.researchAndroid.server.ServerUtility.UNACCEPTED_EXT;
import static com.ten_characters.researchAndroid.server.ServerUtility.USER_EXT;


/**
 * Communicates with the Pallet Server to authenticate users
 * Created by austin on 1/06/15.
 *
 */


public class PalletServer implements ServerAuthenticate, OnTaskCompleted {
    // Todo: Probably should move all the listeners here, send data back, and respond accordingly
    private static final String LOG_TAG = PalletServer.class.getSimpleName();
    private Context mContext;
    private String currentAuthToken;
    private OnTaskCompleted listener;
    private OnFileTaskCompleted fileListener;

    private JsonHttpResponseHandler jsonHandler;
    private FileAsyncHttpResponseHandler fileHandler;
    private File mUploadedFile;
    private int FILE_TIMEOUT = 50000;

    private ProgressDialog mProgressDialog = null;


    /** ONLY TO BE USED FOR userSignIn !!!! DON'T USE FOR ANYTHING CONTEXT RELATED
     * IE WITH THE ANDROID ACCOUNTS */
    public PalletServer() {}

    /** When we really don't care about the response back */
    public PalletServer(Context context) {
        mContext = context;
    }

    public PalletServer(Context context, @Nullable final OnTaskCompleted listener) {
        mContext = context;
        this.listener = listener;

        // For use with the loopj AsyncTask!
        jsonHandler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                if(mProgressDialog != null)
                    mProgressDialog.dismiss();
                onTaskCompleted(response);
            }

            @Override
            public void onProgress(int bytesWritten, int totalSize) {
                if (mProgressDialog != null) {
                    if (!mProgressDialog.isShowing()) {
                        mProgressDialog.setMax(totalSize);
                        mProgressDialog.show();
                    }
                    mProgressDialog.setProgress(bytesWritten);
                }
                super.onProgress(bytesWritten, totalSize);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                // Make sure we delete the file no matter what!
                if (mUploadedFile != null)
                    mUploadedFile.delete();

                if(mProgressDialog != null)
                    mProgressDialog.dismiss();

                if (statusCode == 499) {
                    // This is an NginX status code indicating that indicates the connection was closed
                    // by the device
                    // We still want to finish this, as nothing else is wrong! The file was uploaded! Who cares!
                    // Known occurrences:
                    // Uploading a signed document
                    // If anything other
                    onTaskCompleted(new JSONObject());
                }
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }
        };
    }

    public PalletServer(final Context context, @Nullable final OnFileTaskCompleted fileListener) {
        mContext = context;
        this.fileListener = fileListener;

        // For use with the loopj AsyncTask!
        fileHandler = new FileAsyncHttpResponseHandler(context) {
            @Override
            public void onSuccess(int i, Header[] headers, File file) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
                // Still no authentication for file downloading so no new tokens!
                fileListener.onFileTaskCompleted(file);
            }

            @Override
            public void onProgress(int bytesWritten, int totalSize) {
                if (mProgressDialog != null) {
                    if (!mProgressDialog.isShowing()) {
                        mProgressDialog.setMax(totalSize);
                        mProgressDialog.show();
                    }
                    mProgressDialog.setProgress(bytesWritten);
                }
                super.onProgress(bytesWritten, totalSize);
            }

            @Override
            public void onFailure(int i, Header[] headers, Throwable throwable, File file) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
                Toast.makeText(context, "Failed to download file!", Toast.LENGTH_SHORT).show();
            }
        };
    }

    public void setProgressDialog(ProgressDialog progressDialog) {
        if (mProgressDialog != null)
            mProgressDialog.dismiss();
        mProgressDialog = progressDialog;
    }

    // Consolidates all necessary checks here! Wow! Might even bring the 403 error handling here as well!
    @Override
    public void onTaskCompleted(JSONObject result) {
        // Make sure to delete the file so we don't take up unnecessary space! Ah! Austin! That's me!
        if (mUploadedFile != null)
            mUploadedFile.delete();

        if (mProgressDialog != null)
            mProgressDialog.dismiss();

        if (result == null)
            return;

        try {
            if (result.has(ServerUtility.NEW_TOKEN_KEY)) {
                invalidateAuthToken();
                saveToken(mContext, result.getString(ServerUtility.NEW_TOKEN_KEY));
            }
            else if (result.has(ServerUtility.ERROR_CODE_KEY)) {
                switch (result.getString(ServerUtility.ERROR_CODE_KEY)){
                    case "403":
                        // Having problems accidentally launching this intent when outside of the app
                        // Potentially workaround:
                        // Mainly havign this problem with services running in the background so:
                        // Let's see if we can cast the context as an Activity:
                        // - if not: must be the GlobalApp, thus a service
                        try {
                            ((Activity) mContext).hasWindowFocus();
                            // If we get this far then it must be an activity
                            // Launch Login Intent
                            Intent loginIntent = new Intent(mContext, AuthActivity.class);
                            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            loginIntent.putExtra(AccountUtility.INTENT_UNAUTHENTICATED, true);
                            mContext.startActivity(loginIntent);
                        } catch (ClassCastException e) {
                            ((GlobalApp) mContext).stopAllServices();
                        }
                        break;
                    case "500":
//                        Toast.makeText(mContext, "Server error!", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        } catch (JSONException e) {
            Log.wtf(LOG_TAG, "Couldn't save auth token?!?", e);
        }

        if (listener != null)
            listener.onTaskCompleted(result);
    }

    /** Send an Http request, asking for an authentication response
     * Any exception will be Raised and then marked as an error in the intent */
    @Override
    public String userSignIn(String user, String pass, String authType) throws Exception {
        final String CONTENT_TYPE = "Content-Type";
        final String JSON_TEXT_TYPE = "application/json; charset=utf-8";
        // Todo: Need to make sure the connection is closed no matter what!
        // Send a request out to the server with the email and password
        // If there is a auth token sent back, hooray!
        // If there is a 403 response sent back, oh no :(
        // Raise an exception
        JSONObject requestData = new JSONObject();

        requestData.put(EMAIL_KEY, user);
        requestData.put(PASSWORD_KEY, pass);

        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(LOGIN_EXT)
                .build();

        // Don't use a serverRequestTask to login as the AccountManager already uses an Asynchronous method
        HttpURLConnection connection;
        URL url = new URL(builtURI.toString());
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty(CONTENT_TYPE, JSON_TEXT_TYPE);

        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);

        // Send the request
        DataOutputStream os = new DataOutputStream(connection.getOutputStream());
        os.writeBytes(requestData.toString());
        os.flush();
        os.close();

        // Now we shall get the response back
        // Will either be a token or a 403 response
        // Should be either in the form 'token': token
        // OR if there was an error, the response code
        InputStream is;
        int responseCode = -1;
        JSONObject responseJSON = null;
        try {
            is = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer responseBuffer = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                responseBuffer.append(line + "\n");
            }
            reader.close();
            // Make JSON from the response
            responseJSON = new JSONObject(responseBuffer.toString());
            connection.disconnect();
        }catch (FileNotFoundException e) {
            responseCode = connection.getResponseCode();
        }

        if (responseCode == 403) {
            throw new Exception("Unauthenticated!");
        }

        // Try to return the auth token, will throw an error and deal appropriated if can't find token
        return responseJSON.getString(AUTH_TOKEN_KEY);
    }

    public void sendDownloadedInfo(String email, String phone) {
        final String EMAIL_KEY = "email";
        final String PHONE_KEY = "phone";

        JSONObject data = new JSONObject();
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1)
                .appendPath("downloaded_user")
                .build();

        try {
            data.put(EMAIL_KEY, email);
            data.put(PHONE_KEY, phone);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Couldn't add keys to uploadPayment json!", e);
        }

        ServerRequestTask requestTask = new ServerRequestTask(this);
        requestTask.execute(builtURI.toString(), "POST", data.toString());
    }

    public void uploadPayment(String routing, String account) {
        final String ROUTING_KEY = "routing_number";
        final String ACCOUNT_KEY = "account_number";
        JSONObject userData = new JSONObject();
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(USER_EXT)
                .appendPath(ADD_PAYMENT_EXT)
                .build();

        // Build JSON data
        appendAuthData(userData);

        try {
            userData.put(ROUTING_KEY, routing);
            userData.put(ACCOUNT_KEY, account);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Couldn't add keys to uploadPayment json!", e);
        }

        ServerRequestTask requestTask = new ServerRequestTask(this);
        requestTask.execute(builtURI.toString(), "POST", userData.toString());
    }

    public void logout() {
        final String LOGOUT_EXT = "logout";
        JSONObject userData = new JSONObject();
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(LOGOUT_EXT)
                .build();

        // Build JSON data
        appendAuthData(userData);

        ServerRequestTask requestTask = new ServerRequestTask(this);
        requestTask.execute(builtURI.toString(), "POST", userData.toString());
    }

    public void getCurrentUser() { getCurrentUser(-1, false);}
    public void getCurrentUser(int max, boolean only_current) {
        final String ME_EXT = "me";
        JSONObject userData = new JSONObject();
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1)
                .appendPath(ME_EXT)
                .build();

        // Build JSON data
        appendAuthData(userData);
        try {
            userData.put(MAX_RETURNED_KEY, max);
            userData.put(ONLY_CURRENT_KEY, only_current);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Couldn't add keys to getCurrentUser json!", e);
        }
        ServerRequestTask requestTask = new ServerRequestTask(this);
        requestTask.execute(builtURI.toString(), "PUT", userData.toString());
    }

    public void getUnacceptedShipments(int maxReturned, int range) {
        JSONObject data = new JSONObject();
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1)
                .appendPath(SHIPMENTS_EXT)
                .appendPath(UNACCEPTED_EXT)
                .build();

        // Build JSON data
        appendAuthData(data);

        try {
            if (maxReturned != -1)
                data.put(MAX_RETURNED_KEY, maxReturned);
            if (range != -1)
                data.put(RANGE_KEY, range);
                // Todo: Put current location in the data too!
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Couldn't compile json data.", e);
        }

        ServerRequestTask requestTask = new ServerRequestTask(this);
        requestTask.execute(builtURI.toString(), "PUT", data.toString());
    }
    public void getUnacceptedShipments() {
        getUnacceptedShipments(-1, -1);
    }

    public void getUserShipments(int maxReturned, boolean onlyCurrent) {
        JSONObject userData = new JSONObject();
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1)
                .appendPath(SHIPMENTS_EXT)
                .appendPath(GET_MINE_EXT)
                .build();

        // Build JSON data
        appendAuthData(userData);
        try {
            userData.put(ONLY_CURRENT_KEY, onlyCurrent);
            if (maxReturned != -1)
                userData.put(MAX_RETURNED_KEY, maxReturned);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Couldn't compile json data.", e);
        }

        ServerRequestTask requestTask = new ServerRequestTask(this);
        requestTask.execute(builtURI.toString(), "PUT", userData.toString());
    }
    public void getUserShipments() {
        getUserShipments(-1, true);
    }
    /** Communicate with Server about*/
    public void answerShipmentOffer(boolean wasAccepted, User user, Shipment shipment) {
        final CountDownLatch serverLatch = new CountDownLatch(1);
        final String RESPONSE_KEY = "response";

        JSONObject data = new JSONObject();
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1)
                .appendPath(SHIPMENTS_EXT)
                .appendPath(RESPOND_EXT)
                .build();

        // Try to contact the server
        try {
            // Build JSON data
            appendAuthData(data);
            data.put(DRIVER_ID_KEY, user.getId());
            data.put(SHIPMENT_ID_KEY, shipment.getId());
            data.put(RESPONSE_KEY, wasAccepted);

            ServerRequestTask requestTask = new ServerRequestTask(this);
            requestTask.execute(builtURI.toString(), "PUT", data.toString());
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Couldn't compile json data.", e);
        }

        // Deal with the response on the android side
        if (wasAccepted) {
            Log.d(LOG_TAG, "Accepted the shipment!");
        }
        else {
            Log.d(LOG_TAG, "Declined the shipment!");
            // Just close the shipment activity after contacting server
            // Or launch the main activity again
            // That might keep the offer in the back-stack though
            // We don't want that
        }
    }

    public void pickupShipment(Uri photoUri, Shipment shipment) {
        final String PICKUP_KEY = "pickup";
        JSONObject data = new JSONObject();
        // Try to contact the server
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1)
                .appendPath(SHIPMENTS_EXT)
                .appendPath(PICKUP_KEY)
                .appendPath(shipment.getId())
                .build();

        appendAuthData(data);

        // Zip the file so its nice and small! The server can take it!
        // Well, marginally smaller for most, could be useful later
        try {
            URI uri = new URI(photoUri.toString());
            File photoFile = new File(uri);
            mUploadedFile = GeneralUtility.zipFile(photoFile);
            photoFile.delete();
        } catch (URISyntaxException e) {
            Log.e(LOG_TAG, "Uri syntax error!", e);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem zipping the file!");
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.setConnectTimeout(FILE_TIMEOUT);
        RequestParams params = new RequestParams();
        try {
            if (mUploadedFile != null) {
                params.put("file", mUploadedFile);
                params.put("data", data.toString());
                client.post(builtURI.toString(), params, jsonHandler);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Can't find file to upload!", e);
        }
    }

    public void dropoffShipment(String photoPath, String signeeName, Shipment shipment) {
        final String FINISH_KEY = "finish";
        final String SIGNATURE_FILE_KEY = "signature";
        final String SIGNEE_NAME_KEY = "signee_name";

        JSONObject data = new JSONObject();
        // Try to contact the server
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1_1)
                .appendPath(SHIPMENTS_EXT)
                .appendPath(FINISH_KEY)
                .appendPath(shipment.getId())
                .build();

        appendAuthData(data);

        try {
            File photoFile = new File(photoPath);
            mUploadedFile = GeneralUtility.zipFile(photoFile);
            photoFile.delete();

            data.put(SIGNEE_NAME_KEY, signeeName);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem zipping the file!");
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem appending json!");
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.setConnectTimeout(FILE_TIMEOUT);
        RequestParams params = new RequestParams();
        try {
            params.put(SIGNATURE_FILE_KEY, mUploadedFile);
            params.put("data", data.toString());
            client.post(builtURI.toString(), params, jsonHandler);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Can't find file to upload!", e);
        }
    }

    private void uploadDocPhoto(File photoFile, String docType, @Nullable Shipment shipment) {
        final String UPLOAD_KEY = "upload";
        JSONObject data = new JSONObject();
        // Try to contact the server
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1)
                .appendPath(UPLOAD_KEY)
                .appendPath(docType)
                .build();

        appendAuthData(data);

        if (shipment != null) {
            try {
                data.put(SHIPMENT_ID_KEY, shipment.getId());
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem putting shipment id in JSON!", e);
            }
        }

        // Zip the file so its nice and small! The server can take it!
        try {
            mUploadedFile = GeneralUtility.zipFile(photoFile);
            photoFile.delete();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Couldn't write the zip file!", e);
        }
        // Will potentially move all request tasks to this httpclient
        // Although mine uses a non-depreciated function
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        try {
            params.put("file", mUploadedFile);
            params.put("data", data.toString());
            client.post(builtURI.toString(), params, jsonHandler);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Can't find file to upload!", e);
        }
    }

    public void uploadDocPhoto(String photoPath, Shipment shipment) {
        File photoFile =  new File(photoPath);
        uploadDocPhoto(photoFile, DocumentUtility.BOL_TYPE_KEY, shipment);
    }
    public void uploadDocPhoto(String photoPath, String docType) {
        File photoFile =  new File(photoPath);
        uploadDocPhoto(photoFile, docType, null);
    }
    public void uploadDocPhoto(Uri photoUri, String docType) throws URISyntaxException {
        URI uri = new URI(photoUri.toString());
        File photoFile = new File(uri);
        uploadDocPhoto(photoFile, docType, null);
    }
    public void uploadDocPhoto(URI photoUri, String docType) {
        File photoFile = new File(photoUri);
        uploadDocPhoto(photoFile, docType, null);
    }

    public void downloadFile(String filePath) {
        final String DOWNLOAD_KEY = "download";
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1)
                .appendPath(DOWNLOAD_KEY)
                .appendPath(filePath)
                .build();

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(builtURI.toString(), fileHandler);
    }

    /** Toggle On/Off Duty
     * Requires login */
    public void toggleDuty() {
        final String RESPONSE_KEY = "response";

        JSONObject data = new JSONObject();
        // Try to contact the server
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1)
                .appendPath(DRIVERS_EXT)
                .appendPath(TOG_ACTIVE_EXT)
                .build();

        appendAuthData(data);

        ServerRequestTask requestTask = new ServerRequestTask(this);
        requestTask.execute(builtURI.toString(), "PUT", data.toString());
    }

    public void updateLocation(double lat, double lng, @Nullable Double orientation, float velocity) {
        final String UPDATE_LOCATION_KEY = "update_location";
        final String LOCATION_KEY = "location";
        final String ORIENTATION_KEY = "orientation";
        final String VELOCITY_KEY = "velocity";

        // String response = null;
        JSONObject data = new JSONObject();
        // Try to contact the server
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1)
                .appendPath(DRIVERS_EXT)
                .appendPath(UPDATE_LOCATION_KEY)
                .build();

        appendAuthData(data);
        JSONArray loc_arr = new JSONArray();
        try {
            loc_arr.put(lat);
            loc_arr.put(lng);
            data.put(LOCATION_KEY, loc_arr);

            if (orientation != null) {
                data.put(ORIENTATION_KEY, orientation);
            }

            data.put(VELOCITY_KEY, velocity);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Couldn't add location data to JSON!", e);
        }
        ServerRequestTask requestTask = new ServerRequestTask(this);
        requestTask.execute(builtURI.toString(), "PUT", data.toString());
    }
    public void updateLocation(Location location, @Nullable Double orientation, float velocity) {updateLocation(location.getLatitude(), location.getLongitude(), orientation, velocity);}
    public void updateLocation(LatLng latLng, @Nullable Double orientation, float velocity) {updateLocation(latLng.latitude, latLng.longitude, orientation, velocity);}

    public void reportIssue(boolean canDeliver, Double estimatedDelayHours) {
        final String ISSUE_EXT = "issue";
        final String CAN_DELIVER_KEY = "can_deliver";
        final String ESTIMATED_DELAY_HOURS_KEY = "estimated_delay_hours";

        JSONObject data = new JSONObject();
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1)
                .appendPath(DRIVERS_EXT)
                .appendPath(ISSUE_EXT)
                .build();

        appendAuthData(data);

        try {
            data.put(CAN_DELIVER_KEY, canDeliver);
            if (estimatedDelayHours != -1.0) {
                data.put(ESTIMATED_DELAY_HOURS_KEY, estimatedDelayHours);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Couldn't add issue data to JSON!", e);
        }

        ServerRequestTask requestTask = new ServerRequestTask(this);
        requestTask.execute(builtURI.toString(), "POST", data.toString());
    }
    // Should only use if cannot deliver!
    public void reportIssue(boolean canDeliver) {
        reportIssue(canDeliver, -1.0);
    }


    public void rate(Float pickupRating, Float dropoffRating, Shipment shipment) {
        final String RATE_EXT = "rate";
        final String PICKUP_RATING_KEY = "shipper_rating";
        final String DROPOFF_RATING_KEY = "consignee_rating";

        JSONObject data = new JSONObject();
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1)
                .appendPath(RATE_EXT)
                .appendPath(shipment.getId())
                .build();

        appendAuthData(data);

        try {
            data.put(PICKUP_RATING_KEY, pickupRating);
            data.put(DROPOFF_RATING_KEY, dropoffRating);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Couldn't add issue data to JSON!", e);
        }

        ServerRequestTask requestTask = new ServerRequestTask(this);
        requestTask.execute(builtURI.toString(), "POST", data.toString());
    }


    public void toggleFavorite(String shipmentId) {
        final String FAVORITE_EXT = "favorite";

        JSONObject data = new JSONObject();
        Uri builtURI = Uri.parse(BASE_API_URL).buildUpon()
                .appendPath(API_VERSION_1)
                .appendPath(SHIPMENTS_EXT)
                .appendPath(FAVORITE_EXT)
                .appendPath(shipmentId)
                .build();

        appendAuthData(data);

        ServerRequestTask requestTask = new ServerRequestTask(this);
        requestTask.execute(builtURI.toString(), "PUT", data.toString());
    }

    /** For all account related thingers */
    private Account getCurrentAccount() {
        AccountManager manager = AccountManager.get(mContext);
        Account userAccount = null;
        Account[] accounts = manager.getAccountsByType(AccountUtility.ACCOUNT_TYPE);
        if (accounts.length != 0) {
            userAccount = accounts[0];
        }

        return userAccount;
    }

    private String getCurrentAccountName() {
        Account userAccount = getCurrentAccount();
        if (userAccount != null) {
            return userAccount.name;
        }
        return null;
    }

    private String getCurrentAuthToken() {
        final long tokenTimeoutSeconds = 10;
        final CountDownLatch tokenLatch = new CountDownLatch(1);
        AccountManager manager = AccountManager.get(mContext);
        Account userAccount = getCurrentAccount();
        // Get the auth token
        if (userAccount != null) {
            currentAuthToken = getToken(mContext);
        }
        return currentAuthToken;
    }

    private void appendAuthData(JSONObject data) {
        String email = getCurrentAccountName();
        String token = getCurrentAuthToken();
        // Only append the auth data if they are both there

        if (email != null && token != null) {
            try {
                data.put(EMAIL_KEY, email);
                data.put(AUTH_TOKEN_KEY, token);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem appending Auth Data!", e);
            }
        }

    }

    public void invalidateAuthToken() {
        //TODO!
    }


}

