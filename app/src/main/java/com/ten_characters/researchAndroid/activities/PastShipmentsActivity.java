package com.ten_characters.researchAndroid.activities;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.userInfo.Shipment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/** Here we really just want to display a nice list, launching a ViewShipmentActivity upon click*/

public class PastShipmentsActivity extends ActionBarActivity {

    private static final String LOG_TAG = PastShipmentsActivity.class.getSimpleName();

    private Shipment[] pastShipments;
    private RecyclerView mRecyclerView;
    private PastShipmentRecyclerAdapter mAdapter;

    /** This is used to implement our custom font, initialized in the GlobalApp.java file*/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_shipments);

        // Get the Attached List
        // Todo: Deal with no finished shipments nicely!
        ArrayList<Shipment> shipmentArrayList = (ArrayList) getIntent().getSerializableExtra(GeneralUtility.PAST_SHIPMENT_LIST_INTENT_KEY);
        pastShipments = new Shipment[shipmentArrayList.size()];
        shipmentArrayList.toArray(pastShipments);

        // Now set up the card view!
        mRecyclerView = (RecyclerView) findViewById(R.id.card_recycler_view);

        // Always with this linear layout manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Now let's set the adapter
        // Should we just give the adapter a list of shipments?
        // Should we give it a cursor from an sqlite database where past shipments are stored?
        // oh well, we will give it a some time to simmer
        mAdapter = new PastShipmentRecyclerAdapter(pastShipments);

        mRecyclerView.setAdapter(mAdapter);

        final EditText searchBar = ((EditText) findViewById(R.id.search_bar));

        // Filter the cards on every character entry
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }


    // These classes set up and handle the cards! !
    // We should think about not storing the shipments in objects
    // as that might get bulky when people realy start moving
    private static class PastShipmentRecyclerAdapter extends RecyclerView.Adapter<PastShipmentRecyclerAdapter.PastShipmentViewHolder>
                                                        implements Filterable {
        private static final int TYPE_NO_RESULTS = -1;
        private static final int TYPE_SHIPMENT = 0;

        private final Shipment[] mShipments;
        private final List<Shipment> filteredShipments = new ArrayList<>();
        private static boolean foundResults;

        public PastShipmentRecyclerAdapter(Shipment[] shipments) {
            mShipments = shipments;
            foundResults = true;
        }

        @Override
        public PastShipmentRecyclerAdapter.PastShipmentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == TYPE_SHIPMENT) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_past_shipment, parent, false);
            }
            else
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_no_search_results, parent, false);
            return getHolder(view, viewType);
        }

        @Override
        public void onBindViewHolder(PastShipmentRecyclerAdapter.PastShipmentViewHolder holder, int position) {
            if (holder.viewType == TYPE_NO_RESULTS)
                // If there are no results, the card will not need any extra population!
                return;
            // ELSE TYPE_SHIPMENT
            Shipment boundShipment;
            if (filteredShipments.size() == 0)
                boundShipment = mShipments[position];
            else
                boundShipment = filteredShipments.get(position);

            holder.refView.setText(boundShipment.getPrimaryReferenceNumber());
            holder.pickupView.setText(boundShipment.getPickupAddress().getOfferFormat());
            holder.dropoffView.setText(boundShipment.getDropoffAddress().getOfferFormat());
            holder.dateView.setText(formatDate(boundShipment.getTimeFinished()));
        }

        // A customized holder-getter so we can add the onClick listener
        public PastShipmentViewHolder getHolder(View view, int viewType) {
            return new PastShipmentViewHolder(view, viewType) {
                @Override
                public void onClick(View view) {
                    // Launch a shipment view, query the shipment depending on if this is a filtered view or just plain
                    Intent viewShipmentIntent = new Intent(view.getContext(), ViewShipmentActivity.class);
                    if (filteredShipments.size() == 0)
                        viewShipmentIntent.putExtra(GeneralUtility.SHIPMENT_INTENT_KEY, mShipments[this.getPosition()]);
                    else
                        viewShipmentIntent.putExtra(GeneralUtility.SHIPMENT_INTENT_KEY, filteredShipments.get(this.getPosition()));

                    view.getContext().startActivity(viewShipmentIntent);
                }
            };
        }

        @Override
        public int getItemCount() {
            if (filteredShipments.size() != 0)
                return filteredShipments.size();
            else if (!foundResults)
                // Only one card if we didn't find any results!
                return 1;

            return mShipments.length;
        }

        @Override
        public int getItemViewType(int position) {
            if (!foundResults)
                return TYPE_NO_RESULTS;
            return TYPE_SHIPMENT;
        }

        @Override
        public Filter getFilter() {
            return new PastShipmentFilter(this, mShipments);
        }

        private static String formatDate(Date date) {
            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy");
            return df.format(date);
        }


        /* SECTION */
        /* INNER CLASSES */
        public static class PastShipmentViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            public TextView refView, pickupView, dropoffView, dateView;
            public final int viewType;

            public PastShipmentViewHolder(View itemView, int viewType) {
                super(itemView);
                this.viewType = viewType;
                // Very interesting. Fills up the screen (5), then loads three more (on the Kyocera)
                // As a buffer
                if (viewType == TYPE_SHIPMENT) {
                    refView = ((TextView) itemView.findViewById(R.id.card_reference_number));
                    pickupView = ((TextView) itemView.findViewById(R.id.card_pickup_text));
                    dropoffView = ((TextView) itemView.findViewById(R.id.card_dropoff_text));
                    dateView = ((TextView) itemView.findViewById(R.id.card_date_text));
                    itemView.setOnClickListener(this);
                }
            }

            @Override
            public void onClick(View v) {}
        }

        public static class PastShipmentFilter extends Filter {

            private final PastShipmentRecyclerAdapter mAdapter;
            private final List<Shipment> mShipmentDataset;

            private List<Shipment> filteredList = new ArrayList<>();

            public PastShipmentFilter(PastShipmentRecyclerAdapter adapter, Shipment[] dataset) {
                mAdapter = adapter;
                // Saves space, marginally, but still, by using arrays where we can
                mShipmentDataset = new ArrayList<>(Arrays.asList(dataset));
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                // Perhaps should be in an async task for larger datasets?
                filteredList.clear();

                final FilterResults results = new FilterResults();

                if (constraint.length() == 0 ) {
                    // Clear the list, no sense in storing the same data in two separate lists
                    filteredList.clear();
                    foundResults = true;
                } else {
                    try {
                        // For now we will just support reference numbers, time finished, pickups, and dropoffs
                        // It would be cool in the future to support many search fields at once
                        for (Shipment shipment : mShipmentDataset) {
                            if (shipment.getPrimaryReferenceNumber().contains(constraint)
                                    || shipment.getPickupAddress().getOfferFormat().toLowerCase().contains(constraint)
                                    || shipment.getDropoffAddress().getOfferFormat().toLowerCase().contains(constraint)
                                    || formatDate(shipment.getTimeFinished()).contains(constraint))
                                filteredList.add(shipment);
                        }

                        // If we have gotten this far and still don't have any results
                        // then we're shure as shite not getting any
                        if (filteredList.size() == 0)
                            foundResults = false;
                        else
                            foundResults = true;

                    } catch (NumberFormatException e) {
                        filteredList.clear();
                        // Just going to display aaall the shipments
                        // if there was a data error
                        // Could consider not doing this
                        // persuade me
                        foundResults = true;
                    }
                }

                results.values = filteredList;
                results.count = filteredList.size();

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mAdapter.filteredShipments.clear();
                mAdapter.filteredShipments.addAll((ArrayList<Shipment>) results.values);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

}
