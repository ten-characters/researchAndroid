package com.ten_characters.researchAndroid;

import android.content.Intent;
import android.support.v4.app.LoaderManager;
import android.content.Context;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ten_characters.researchAndroid.activities.OfferShipmentActivity;
import com.ten_characters.researchAndroid.data.PalletDbContract;
import com.ten_characters.researchAndroid.userInfo.Shipment;

/**
 * Created by austin on 8/19/15.
 */
public class FavoritesDrawerHandler {
    private final Context mContext;
    private final RecyclerView mRecyclerView;
    private DrawerAdapter mAdapter;
    private final DrawerLayout mDrawerLayout;
    public ActionBarDrawerToggle mDrawerToggle;
    private final LoaderManager mLoaderManager;

    private static final int FAVORITE_LOADER_ID = 3;

    public FavoritesDrawerHandler(Context context, LoaderManager loaderManager, DrawerLayout drawerLayout, RecyclerView recyclerView) {
        mContext = context;
        mLoaderManager = loaderManager;
        mDrawerLayout = drawerLayout;
        mRecyclerView = recyclerView;
        setupDrawer();
    }

    private void setupDrawer() {

        mRecyclerView.setHasFixedSize(false);
        mAdapter = new DrawerAdapter(mContext);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        mLoaderManager.initLoader(FAVORITE_LOADER_ID, null, mAdapter);

        /*mDrawerToggle = new ActionBarDrawerToggle(
                (Activity) mContext,
                mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        );*/
    }

    /** This is what actually builds the drawer!
     * I want to populate this from a cursor, provided by the content provider */
    public static class DrawerAdapter extends RecyclerView.Adapter<DrawerAdapter.FavoriteViewHolder>
            implements LoaderManager.LoaderCallbacks<Cursor>{
        private static final String LOG_TAG = DrawerAdapter.class.getSimpleName();

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_SHIPMENT = 1;
        private static final int TYPE_BLANK_INFO = 2;

        private final Context mContext;

        private Cursor mFavoritesCursor;

        public DrawerAdapter(Context context) {
            mContext = context;
        }

        @Override
        public FavoriteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case TYPE_HEADER: 
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fav_header, parent, false);
                    break;
                case TYPE_SHIPMENT: 
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_fav_shipment, parent, false);
                    break;
                case TYPE_BLANK_INFO: 
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_fav_none_found, parent, false);
                    break;
                default:
                    return null;
            }
            
            return getHolder(view, viewType);
        }

        @Override
        public void onBindViewHolder(FavoriteViewHolder holder, int position) {
            if (holder.holderType == TYPE_SHIPMENT) {
                // Really the only dynamically changinging element

                // Find the shipment and populate
                try {
                    Shipment shipment = new Shipment();
                    mFavoritesCursor.moveToPosition(position - 1);
                    shipment.setupFromFullProjection(mFavoritesCursor);

                    /*double hoursToAvailable = shipment.getIsAvailableInHours();
                    if (hoursToAvailable > 0)
                        holder.availableText.setText("Available in " + shipment.getIsAvailableInHours() + " hours.");
                    else
                        holder.availableText.setText("Available now!");*/

                    holder.priceText.setText(GeneralUtility.getFormattedPrice(mContext, shipment.getDoublePrice()));

                    holder.pickupLocText.setText(GeneralUtility.getFormattedAddressCityState(mContext, shipment.getPickupAddress()));
                    holder.pickupTimeText.setText(GeneralUtility.getFormattedDate(mContext, shipment.getPickupTime()));

                    holder.dropoffLocText.setText(GeneralUtility.getFormattedAddressCityState(mContext, shipment.getDropoffAddress()));
                    holder.dropoffTimeText.setText(GeneralUtility.getFormattedDate(mContext, shipment.getDropoffTime()));

                } catch (GeneralUtility.DateException e) {
                    Log.e(LOG_TAG, "Couldn't parse date in favorite shipment!", e);
                }
            }
        }

        @Override
        public int getItemCount() {
            // Always a header and then:
            // Length of all the toggleFavorite shipments or, one, if one, just the blank toggleFavorite info card
            if (mFavoritesCursor == null)
                return 1;

            return (mFavoritesCursor.getCount() != 0) ? mFavoritesCursor.getCount() + 1 : 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0)
                return TYPE_HEADER;

            if (mFavoritesCursor.getCount() != 0) // Gotta be a shipments
                return TYPE_SHIPMENT;

            return TYPE_BLANK_INFO;
        }

        public FavoriteViewHolder getHolder(View view, int viewType) {
            return new FavoriteViewHolder(view, viewType) {
                @Override
                public void onClick(View v) {
                    Intent viewOfferIntent = new Intent(mContext, OfferShipmentActivity.class);
                    Shipment clickedShipment = new Shipment();
                    try {
                        // Adjust the cursor to the position of the shipment
                        // accounting for the header
                        // the ever present header
                        // never leaves
                        // been here since August 2015
                        // never got the hint
                        mFavoritesCursor.moveToPosition(this.getPosition() - 1);
                        clickedShipment.setupFromFullProjection(mFavoritesCursor);
                        viewOfferIntent.putExtra(GeneralUtility.SHIPMENT_INTENT_KEY, clickedShipment);
                        mContext.startActivity(viewOfferIntent);
                    } catch (GeneralUtility.DateException e) {
                        Log.e(LOG_TAG, "Couldn't parse date in favorite shipment!", e);
                    }
                }
            };
        }

        /** All the loader good stuff that will be updated with the content provider! */
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Uri favoriteShipmentsUri = PalletDbContract.FavoriteShipmentEntry.buildFavoriteShipmentsUri();
            return new CursorLoader(mContext,
                    favoriteShipmentsUri,
                    Shipment.sFullProjection,
                    null,
                    null,
                    null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            // Now we have all of the favorites shipments!
            // Thanks special content provider!
            mFavoritesCursor = data;
            // Once we get new data, we have to change what's here!
            notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mFavoritesCursor = null;
        }

        public static class FavoriteViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            // This is where each specific view gets held, duh!
            // Really just separates which type of thinger we are populating
            // in the onBindViewHolder

            private int holderType;

            private TextView priceText, pickupLocText, pickupTimeText, dropoffLocText, dropoffTimeText, availableText;

            public FavoriteViewHolder(View itemView, int viewType) {
                super(itemView);
                holderType = viewType;
                // Either the header or a row item!
                switch (viewType) {
                    case TYPE_HEADER:
                        break;
                    case TYPE_SHIPMENT:
                        CardView shipmentCard = (CardView) itemView.findViewById(R.id.fav_card_view);
                        // When a shipment is clicked we want to bring up it's offer screen
                        shipmentCard.setOnClickListener(this);
                        // Now get all the textViews
                        priceText = (TextView) itemView.findViewById(R.id.favorite_price_text);

                        pickupLocText = (TextView) itemView.findViewById(R.id.card_pickup_location_text);
                        pickupTimeText = (TextView) itemView.findViewById(R.id.card_pickup_time_text);

                        dropoffLocText = (TextView) itemView.findViewById(R.id.card_dropoff_location_text);
                        dropoffTimeText = (TextView) itemView.findViewById(R.id.card_dropoff_time_text);

                        availableText = (TextView) itemView.findViewById(R.id.favorite_available_in_text);
                        break;
                    case TYPE_BLANK_INFO:
                        break;
                }
            }


            @Override
            public void onClick(View v) {}
        }
    }
}
