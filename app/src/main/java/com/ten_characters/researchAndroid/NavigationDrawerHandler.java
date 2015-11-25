package com.ten_characters.researchAndroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.View;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ten_characters.researchAndroid.activities.AboutActivity;
import com.ten_characters.researchAndroid.activities.AuthActivity;
import com.ten_characters.researchAndroid.activities.PastShipmentsActivity;
import com.ten_characters.researchAndroid.activities.ProfileActivity;
import com.ten_characters.researchAndroid.activities.PromoActivity;
import com.ten_characters.researchAndroid.activities.UploadPaymentActivity;
import com.ten_characters.researchAndroid.userInfo.User;

import java.io.File;

import static com.ten_characters.researchAndroid.auth.AccountUtility.INTENT_LOGOUT;

/**
 * Created by austin on 7/16/15.
 */
public class NavigationDrawerHandler {

    private static final String LOG_TAG = NavigationDrawerHandler.class.getSimpleName();

    private Context mContext;
    private RecyclerView mRecyclerView;
    public ActionBarDrawerToggle mDrawerToggle;

    private DrawerAdapter mAdapter;
    private DrawerLayout mDrawerLayout;
    private File mPropicFile;
    private String[] mListOptions;
    private int[] mListIcons;
    private User mUser;

    public NavigationDrawerHandler(Context context, DrawerLayout drawerLayout,
                                   RecyclerView recyclerView, User user) {
        mContext = context;

        mRecyclerView = recyclerView;
        mDrawerLayout = drawerLayout;
        mUser = user;

        setupDrawer();
    }

    private void setupDrawer() {
        // Get the list for setup!
        mListOptions = mContext.getResources().getStringArray(R.array.main_drawer_items);

        TypedArray images = mContext.getResources().obtainTypedArray(R.array.main_drawer_icons);
        int[] temp = new int[images.length()];
        for (int i = 0; i < images.length(); i++) {
            temp[i] = images.getResourceId(i, 0);
        }
        mListIcons = temp;


        // Just the basic setup for the drawer!
        mRecyclerView.setHasFixedSize(true); // Don't want it changing now do we?

        // Try to get the propic if there!
        mPropicFile = GeneralUtility.getProfilePictureFile(mContext);
        // If the profile picture wasn't there, there will be a default profile picture set!
        mAdapter = new DrawerAdapter(mContext, mUser, mListOptions, mListIcons, mPropicFile);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        // Create the opener closer interfacer
        mDrawerToggle = new ActionBarDrawerToggle(
                (Activity)mContext,
                mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        ){
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                // Log.v(LOG_TAG,"DRAWER CLOSED!");
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                // Log.v(LOG_TAG, "DRAWER OPENED!");
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }

    // This allows for the lil hamburger icon to flip back and forth to an arrow
    public void syncTogState() {
        mDrawerToggle.syncState();
    }

    public void recycleProfileBitmap() {
        mAdapter.recycleBitmap();
    }

    public void setProfilePicture(File profilePicture) {
        mPropicFile = profilePicture;
        mAdapter.setupProfileBitmap();
    }

    /** This is what actually builds the drawer! */
    public static class DrawerAdapter extends RecyclerView.Adapter<DrawerAdapter.ViewHolder>{
        private static final String LOG_TAG = DrawerAdapter.class.getSimpleName();

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;

        private static final int HEADER_MIN_HEIGHT = 172;
        private static final int HEADER_DESIRED_HEIGHT = 192;
        private static final int ROW_DESIRED_HEIGHT = 38;
        private int mRowHeight;
        private int mHeaderHeight;

        private static Context mContext;
        private static User mUser;
        private String[] mDrawerOptions;
        private int[] mDrawerIconIds;

        private File mPropicFile;
        private Bitmap mProfileBitmap;

        public DrawerAdapter(Context context, User user, String[] drawerOptions, int[] drawerIconIds, @Nullable File propicFile) {
            mContext = context;
            mUser = user;
            mDrawerOptions = drawerOptions;
            mDrawerIconIds = drawerIconIds;
            mPropicFile = propicFile;

            setupProfileBitmap();

            // Get screen height and calculate the
            Resources res = mContext.getResources();
            Display display = ((Activity)mContext).getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int heightAvailable = (int)(size.y - res.getDimension(R.dimen.actionbar_height));
            int proposedItemTotalHeight = mDrawerOptions.length * ROW_DESIRED_HEIGHT;
            if (!(proposedItemTotalHeight + HEADER_DESIRED_HEIGHT <= heightAvailable) ) {
                Log.d(LOG_TAG, "Start to shrink!");
            }
        }

        public void recycleBitmap() {
            if (mProfileBitmap != null) {
                mProfileBitmap.recycle();
            }
        }

        public void setupProfileBitmap() {
            if (mPropicFile == null)
                mProfileBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_pallet_logo);
            else {
                // Really trying to deal with memory here
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                mProfileBitmap = BitmapFactory.decodeFile(mPropicFile.getAbsolutePath(), options);
            }
        }

        @Override
        public DrawerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case TYPE_HEADER: {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_header, parent, false);
                    ViewHolder viewHolder = new ViewHolder(view, viewType);
                    return viewHolder;
                }
                case TYPE_ITEM: {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_item, parent, false);
                    ViewHolder viewHolder = new ViewHolder(view, viewType);
                    return viewHolder;
                }
                default:
                    return null;
            }
        }

        @Override
        public void onBindViewHolder(DrawerAdapter.ViewHolder holder, int position) {
            if(holder.holderType == TYPE_HEADER) {
                holder.profileTextView.setText(mUser.getDisplayName());
                holder.profileImageView.setImageBitmap(mProfileBitmap);
            } else {
                holder.rowTextView.setText(mDrawerOptions[position - 1]);
                holder.rowImageView.setImageResource(mDrawerIconIds[position-1]);
            }
        }

        @Override
        public int getItemCount() {
            // Length of all the options plus the header
            return mDrawerOptions.length + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) // Header always at index numba 0
                return TYPE_HEADER;
            return TYPE_ITEM;
        }


        public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            // This is where each specific view gets held, duh!
            // Really just separates which type of thinger we are populating
            // in the onBindViewHolder!
            TextView profileTextView;
            ImageView profileImageView;
            TextView rowTextView;
            ImageView rowImageView;

            private int holderType;

            public ViewHolder(View itemView, int viewType) {
                super(itemView);
                holderType = viewType;
                // Either the header or a row item!
                if(holderType == TYPE_HEADER) {
                    RelativeLayout headerContainer = (RelativeLayout) itemView.findViewById(R.id.drawer_header_container);
                    headerContainer.setOnClickListener(this);
                    profileTextView = (TextView) itemView.findViewById(R.id.profile_name);
                    profileImageView = (ImageView) itemView.findViewById(R.id.profile_image);
                } else {
                    RelativeLayout itemContainer = (RelativeLayout) itemView.findViewById(R.id.drawer_item_container);
                    itemContainer.setOnClickListener(this);
                    rowTextView = (TextView) itemView.findViewById(R.id.row_title);
                    rowImageView = (ImageView) itemView.findViewById(R.id.row_icon);
                }
            }


            // This is where the drawer is actually used to navigate!
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.drawer_header_container) {
                    Intent profileIntent = new Intent(mContext, ProfileActivity.class);
                    profileIntent.putExtra(GeneralUtility.USER_INTENT_KEY, mUser);
                    mContext.startActivity(profileIntent);
                    return;
                }

                // Sort by the string in the rows view!
                String selected = (String)rowTextView.getText();
                if (selected.equals(mContext.getString(R.string.payment))) {
                    Intent paymentIntent = new Intent(mContext, UploadPaymentActivity.class);
                    mContext.startActivity(paymentIntent);
                }
                /*else if (selected.equals(mContext.getString(R.string.current_shipments))) {
                    if (mUser.getCurrentShipments().length == 0) {
                        Toast.makeText(mContext, "You're not currently on a shipment!", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent currentShipmentIntent = new Intent(mContext, ViewShipmentActivity.class);
                        currentShipmentIntent.putExtra(
                                GeneralUtility.SHIPMENT_INTENT_KEY,
                                ((GlobalApp) ((Activity) mContext).getApplication()).getCurrentShipment()
                        );
                        mContext.startActivity(currentShipmentIntent);
                    }
                }*/
                else if (selected.equals(mContext.getString(R.string.past_shipments))) {
                    if (mUser.getPastShipmentsArr().length == 0) {
                        Toast.makeText(mContext, "You don't have any past shipments..yet!", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent pastIntent = new Intent(mContext, PastShipmentsActivity.class);
                        pastIntent.putExtra(GeneralUtility.PAST_SHIPMENT_LIST_INTENT_KEY, mUser.getPastShipmentsArrList());
                        mContext.startActivity(pastIntent);
                    }
                }
                else if (selected.equals(mContext.getString(R.string.about))) {
                    Intent aboutIntent = new Intent(mContext, AboutActivity.class);
                    mContext.startActivity(aboutIntent);
                }
                else if (selected.equals(mContext.getString(R.string.promos))) {
                    Intent promoIntent = new Intent(mContext, PromoActivity.class);
                    promoIntent.putExtra(GeneralUtility.PROMOCODE_INTENT_KEY, mUser.getPromoCode());
                    mContext.startActivity(promoIntent);
                }
                else if (selected.equals(mContext.getString(R.string.logout))) {
                    Intent logoutIntent = new Intent(mContext, AuthActivity.class);
                    logoutIntent.putExtra(INTENT_LOGOUT, true);
                    mContext.startActivity(logoutIntent);
                }
            }
        }
    }
}
