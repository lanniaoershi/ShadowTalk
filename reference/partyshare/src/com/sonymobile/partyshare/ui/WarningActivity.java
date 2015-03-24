/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.sonymobile.partyshare.R;

public class WarningActivity extends Activity {
    public static final String EXTRA_KIND = "extra_kind";
    public static final int WARNING_KIND_UPDATE = 0;
    public static final int WARNING_KIND_NOT_OWNER = 1;

    private AlertDialog mAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int kind = getIntent().getIntExtra(EXTRA_KIND, WARNING_KIND_UPDATE);
        showWarningDialog(kind);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }

    protected void showWarningDialog(int kind) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_layout_textview, null);
        TextView textView = (TextView)view.findViewById(R.id.dialog_text);
        textView.setGravity(Gravity.LEFT);
        int resId = R.string.party_share_strings_update_warning_txt;
        if (kind == WARNING_KIND_NOT_OWNER) {
            resId = R.string.party_share_strings_err_multiuser_not_owner_txt;
        }
        textView.setText(getResources().getText(resId));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mAlertDialog = builder.setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .create();
        mAlertDialog.setCanceledOnTouchOutside(false);
        mAlertDialog.show();
    }
}
