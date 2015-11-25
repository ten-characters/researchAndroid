package com.ten_characters.researchAndroid.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.GlobalApp;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.server.OnFileTaskCompleted;
import com.ten_characters.researchAndroid.server.OnTaskCompleted;
import com.ten_characters.researchAndroid.server.PalletServer;
import com.ten_characters.researchAndroid.server.ServerUtility;
import com.ten_characters.researchAndroid.userInfo.User;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.ten_characters.researchAndroid.DocumentUtility.CAMERA_INTENT_CODE;
import static com.ten_characters.researchAndroid.DocumentUtility.GALLERY_INTENT_CODE;

// Trying out this new thing where i implement the class instead of storing a listener in a global
public class ProfileActivity extends ActionBarActivity implements OnTaskCompleted{

    private static final String LOG_TAG = ProfileActivity.class.getSimpleName();
    private static final String ACTIVITY_NAME = "Profile";

    private PalletServer mServer;

    private User mUser;
    private ImageView profileImageView;
    private File tempImageFile;

    private RecyclerView statRecycler, equipRecycler;

    private static Tracker mTracker;

    /** This is used to implement our custom font, initialized in the GlobalApp.java file*/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        mServer = new PalletServer(this, this);
        profileImageView = (ImageView) findViewById(R.id.profile_image);
        // Query for data and then build the views
        mUser = (User) getIntent().getSerializableExtra(GeneralUtility.USER_INTENT_KEY);

        statRecycler = (RecyclerView) findViewById(R.id.stat_recycler_view);
        //equipRecycler = (RecyclerView) findViewById(R.id.equip_recycler_view);
        try {
            tempImageFile = GeneralUtility.createTemporaryFile("temp_propic", ".jpg"); //new File(this.getFilesDir() + "/temp_pic.jpg");
            tempImageFile.setWritable(true);
        } catch (IOException e) {}

        mTracker = ((GlobalApp) getApplication()).getDefaultTracker();
        mTracker.setScreenName(ACTIVITY_NAME);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        buildView();
    }

    private void buildView() {
        // GET TO THE PROFILE PICTURE !
        Bitmap propic = GeneralUtility.getProfilePictureBitmap(this);
        if (propic != null)
            profileImageView.setImageBitmap(propic);
        else
            profileImageView.setImageResource(R.mipmap.ic_pallet_logo);

        // Set some user based stuffs
        ((TextView) findViewById(R.id.profile_name)).setText(mUser.getDisplayName());
        ((RatingBar) findViewById(R.id.user_rating_bar)).setRating(mUser.getRating());

        // Create the card recyclers!
        // Set them both as horizontal views
        statRecycler.setLayoutManager(new org.solovyev.android.views.llm.LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        statRecycler.setHasFixedSize(true);
        //equipRecycler.setLayoutManager(new org.solovyev.android.views.llm.LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        //equipRecycler.setHasFixedSize(true);

        statRecycler.setAdapter(new StatRecyclerAdapter(mUser.getProfileInfoArrayList()));
        //equipRecycler.setAdapter(new EquipRecyclerAdapter(this, mUser.getEquipmentInfoArrayList()));

        // A temp layout and temp downloads
        PalletServer truckServer = new PalletServer(this, new OnFileTaskCompleted() {
            @Override
            public void onFileTaskCompleted(File result) {
                // Once the file is downloaded, set the image up!
                ((ImageView) findViewById(R.id.truck_image)).setImageBitmap(BitmapFactory.decodeFile(result.getAbsolutePath()));
                result.delete(); // Try to save space where we can
            }
        });
         truckServer.downloadFile(mUser.getFirstTruck().photoPath);

        // A temp layout and temp downloads
        PalletServer trailerServer = new PalletServer(this, new OnFileTaskCompleted() {
            @Override
            public void onFileTaskCompleted(File result) {
                // Once the file is downloaded, set the image up!
                ((ImageView) findViewById(R.id.trailer_image)).setImageBitmap(BitmapFactory.decodeFile(result.getAbsolutePath()));
                result.delete(); // Try to save space where we can
            }
        });
        trailerServer.downloadFile(mUser.getFirstTrailer().photoPath);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.profile_image:
                // Will flow down, thus either clicking the image or the text
                // should start the intent
            case R.id.edit_profile_text:
                // Launch Dialog to pick photo from gallery or to
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setTitle("Take photo or choose from gallery!");
                alertBuilder.setPositiveButton("Gallery", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempImageFile));
                        startActivityForResult(galleryIntent, GALLERY_INTENT_CODE);
                    }
                });
                alertBuilder.setNegativeButton("Take Photo!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempImageFile));
                        startActivityForResult(takePhotoIntent, CAMERA_INTENT_CODE);
                    }
                });
                alertBuilder.show();

                break;
        }
    }

    /** For profile picture results */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            switch (requestCode) {
                case CAMERA_INTENT_CODE: {
                    Bitmap imageBitmap = BitmapFactory.decodeFile(tempImageFile.getAbsolutePath(), options);
                    profileImageView.setImageBitmap(imageBitmap);
                    break;
                }
                case GALLERY_INTENT_CODE: {
                    // Use the content provider to get the selected gallery picture!
                    Uri selectedImageUri = data.getData();
                    String[] fileColumn = { MediaStore.Images.Media.DATA };

                    Cursor imageCursor = getContentResolver().query(selectedImageUri,
                            fileColumn, null, null, null);
                    imageCursor.moveToFirst();

                    int fileColumnIndex = imageCursor.getColumnIndex(fileColumn[0]);
                    String picturePath = imageCursor.getString(fileColumnIndex);
                    try {
                        GeneralUtility.copyFile(new File(picturePath), tempImageFile);
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Couldn't copy file from Gallery!");
                    }

                    Bitmap imageBitmap = BitmapFactory.decodeFile(tempImageFile.getAbsolutePath(), options);
                    profileImageView.setImageBitmap(imageBitmap);
                    break;
                }
            }

            try {
                GeneralUtility.saveProfilePicture(this, tempImageFile);
                mServer.uploadDocPhoto(Uri.fromFile(tempImageFile), ServerUtility.PROFILE_IMAGE_TYPE);
            } catch (URISyntaxException e) {}

            /*try {
                // Store the photo so we can save it internally
                // This isn't the best quality photo, but we don't want to have to save to external devices
                // So this is the best option RIGHT NOW
                // Upload the image to the server!
                String photoPath = GeneralUtility.saveToInternalStorage(this, imageBitmap);
                // Store the image so we can locally store it !
                GeneralUtility.copyFile(new File(photoPath), tempImageFile);
                // Upload the image!

            }  catch (FileNotFoundException e) {
                Log.e(LOG_TAG, "Failed to save document photo internally!", e);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to close stream!", e);
            }*/
        }
    }

    @Override
    public void onTaskCompleted(JSONObject result) {
        Toast.makeText(this, "Successfully uploaded!", Toast.LENGTH_SHORT).show();
    }

    // The adapter that builds the stat view!
    private static class StatRecyclerAdapter extends RecyclerView.Adapter<StatRecyclerAdapter.ViewHolder> {
        private final List<Map<String, String>> mInfoMap;

        public StatRecyclerAdapter(List<Map<String, String>> infoMap) {
            mInfoMap = infoMap;
        }

        @Override
        public StatRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_profile_stat, parent, false), viewType);
        }

        @Override
        public void onBindViewHolder(StatRecyclerAdapter.ViewHolder holder, int position) {
            holder.headerView.setText(mInfoMap.get(position).get(GeneralUtility.HEADER_KEY));
            holder.movesView.setText(mInfoMap.get(position).get(GeneralUtility.MOVES_KEY));
            holder.profitView.setText(mInfoMap.get(position).get(GeneralUtility.PROFIT_KEY));
        }

        @Override
        public int getItemCount() {
            return mInfoMap.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView headerView, movesView, profitView;
            public ViewHolder(View itemView, int viewType) {
                super(itemView);
                headerView = (TextView) itemView.findViewById(R.id.card_header);
                movesView = (TextView) itemView.findViewById(R.id.card_num_moves_text);
                profitView = (TextView) itemView.findViewById(R.id.card_profit_text);
            }
        }
    }

    // The adapter that builds the stat view!
    private static class EquipRecyclerAdapter extends RecyclerView.Adapter<EquipRecyclerAdapter.ViewHolder> {

        private final Context mContext;
        private final List<LinkedHashMap<String, String>>  mEquipList;

        public EquipRecyclerAdapter(Context context, List<LinkedHashMap<String, String>> equipList) {
            mContext = context;
            mEquipList = equipList;
        }

        @Override
        public EquipRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_profile_equipment, parent, false), viewType);
        }

        @Override
        public void onBindViewHolder(final EquipRecyclerAdapter.ViewHolder holder, int position) {
            final LinkedHashMap<String, String> equipInfo = mEquipList.get(position);
            // Download each picture!
            PalletServer downloadServer = new PalletServer(mContext, new OnFileTaskCompleted() {
                @Override
                public void onFileTaskCompleted(File result) {
                    // Once the file is downloaded, set the image up!
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    holder.equipImageView.setImageBitmap(BitmapFactory.decodeFile(result.getAbsolutePath()));
                    result.delete(); // Try to save space where we can
                }
            });
            downloadServer.downloadFile(equipInfo.get(ServerUtility.PHOTO_PATH_KEY));

            // Add a textview for each pertinent piece of info
            // i.e. not the photo path key
            for (String key: equipInfo.keySet()) {
                if (key.equals(ServerUtility.PHOTO_PATH_KEY))
                    break;
                TextView pieceOfInfo = (TextView) LayoutInflater.from(mContext)
                        .inflate(R.layout.card_textview_simple, null);
                pieceOfInfo.setText(equipInfo.get(key));
                holder.equipInfoContainer.addView(pieceOfInfo);
            }
        }

        @Override
        public int getItemCount() {
            return mEquipList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView equipImageView;
            private LinearLayout equipInfoContainer;
            private static String photoPath;
            public ViewHolder(View itemView, int viewType) {
                super(itemView);
                equipImageView = (ImageView) itemView.findViewById(R.id.card_equip_img);
                equipInfoContainer = (LinearLayout) itemView.findViewById(R.id.card_equip_info_container);
            }
        }
    }
}
