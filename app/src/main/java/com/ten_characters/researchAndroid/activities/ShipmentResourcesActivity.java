package com.ten_characters.researchAndroid.activities;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;

import com.ten_characters.researchAndroid.GeneralUtility;
import com.ten_characters.researchAndroid.R;
import com.ten_characters.researchAndroid.server.ServerUtility;
import com.ten_characters.researchAndroid.userInfo.Shipment;

public class ShipmentResourcesActivity extends ActionBarActivity implements View.OnClickListener{

    private Shipment mShipment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipment_resources);
        mShipment = (Shipment) getIntent().getSerializableExtra(ServerUtility.SHIPMENT_KEY);

        buildView();
    }

    public void buildView() {
        if (!mShipment.getBolPath().equals("")) {
            (findViewById(R.id.view_bol_button)).setVisibility(View.VISIBLE);
            (findViewById(R.id.view_bol_button)).setOnClickListener(this);

        }
    }

    @Override
    public void onClick(View button) {
        switch (button.getId()) {
            case R.id.view_bol_button:
                Intent imageviewIntent = new Intent(ShipmentResourcesActivity.this, ViewImageActivity.class);
                imageviewIntent.putExtra(GeneralUtility.FILENAME_INTENT_KEY, mShipment.getBolPath());
                startActivity(imageviewIntent);
                break;
        }
    }
}
