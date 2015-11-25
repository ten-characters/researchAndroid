package com.ten_characters.researchAndroid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by austin on 7/19/15.
 */
public class InstructionsDialogFragment extends DialogFragment {

    private String mTitle, mMessage;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
        alertBuilder.setTitle(R.string.sign_instructions_title);
        alertBuilder.setMessage(R.string.sign_instructions);
        alertBuilder.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        return alertBuilder.create();
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        String type = args.getString(GeneralUtility.INSTRUCTS_DIALOG_TYPE);
        if (type.equals(getString(R.string.pickup))) {
            mTitle = getString(R.string.pickup_instructions_title);
            mMessage = getString(R.string.pickup_instructions);
        }
        else if (type.equals(getString(R.string.dropoff))) {
            mTitle = getString(R.string.sign_instructions_title);
            mMessage = getString(R.string.sign_instructions);
        }
    }
}