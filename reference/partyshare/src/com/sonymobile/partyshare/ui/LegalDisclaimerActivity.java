/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.util.Setting;

/**
 * This class provides functionality for displaying a configurable legal
 * disclaimer.
 *
 * Usage:
 *
 * 1. Create intent:
 *
 *     Intent disclamerIntent = new Intent(this, LegalDisclaimerActivity.class);
 *
 * 2. Select appropriate disclaimer using bit mask:
 *
 *     disclamerIntent.putExtra(LegalDisclaimerActivity.DISCLAMER_SELECTION_EXTRA,
 *                              LegalDisclaimerActivity.TERMS_OF_USE |
 *                              LegalDisclaimerActivity.DATA_CHARGES | * ...);
 *
 * 3. Start Activity:
 *
 *     startActivity(disclamerIntent);
 *
 */
public class LegalDisclaimerActivity extends Activity {

    /**
     * Only display Terms of service
     */
    public static final int TERMS_OF_USE = 0x1;

    /**
     * If the app may cause data charges for the user, e.g. if INTERNET ACCESS
     * is enabled this must be enabled.
     */
    public static final int DATA_CHARGES = 0x2;

    /**
     * When collecting personal data it is recommended to provide information
     * about what data is collected.
     */
    public static final int CUSTOM_PERSONAL_DATA_MESSAGE = 0x4;

    /**
     * If the app is collecting personal data the app must set PERSONAL_DATA so
     * that the privacy policy is included.
     */
    public static final int PERSONAL_DATA = 0x8;

    /**
     * The key name for the disclaimer selection mask
     */
    public static final String DISCLAMER_SELECTION_EXTRA = "discselect";

    /**
     * The key name for the custom privacy text
     */
    public static final String DISCLAMER_CUSTOM_PERSONAL_DATA_TEXT_EXTRA = "personaldata";

    /**
     * Private fields
     */
    private AlertDialog mAlert;

    /** Controls the check box state if orientation changes */
    private boolean mIsChecked = false;

    /** Wifi join info. */
    private String mSsid;
    private String mPassPhrase;
    private String mHostAddress;
    private String mSessionName;
    private String mStartTime;
    private boolean mInvitedByHost;
    private boolean mPermission;

    /**
     ****************************************************************************
     */

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void onResume() {
        super.onResume();

        setWifiJoinInfo(getIntent());
        int type = getIntent().getExtras().getInt(DISCLAMER_SELECTION_EXTRA);
        String personalDataMessage =
                getIntent().getExtras().getString(DISCLAMER_CUSTOM_PERSONAL_DATA_TEXT_EXTRA);

        if (!isDisclamerAccepted(this)) {
            displayLegalDisclamer(this, type, personalDataMessage);
        } else {
            finish();
        }
    }

    /**
     * This method displays the legal disclaimer which should be displayed when
     * a user starts the application the first time. The user should not be
     * allowed to interact with the application before the terms have been
     * accepted.
     *
     * If the terms are not accepted then this method will cause the application
     * to exit.
     *
     */
    private void displayLegalDisclamer(
            final Context context, final int type, final String personalDataMessage) {

        if ((type < TERMS_OF_USE) || (type > (PERSONAL_DATA | DATA_CHARGES | TERMS_OF_USE
                 | CUSTOM_PERSONAL_DATA_MESSAGE))) {
            throw new IllegalArgumentException();
        }

        boolean checkbox = ((type & PERSONAL_DATA) != 0) || ((type & TERMS_OF_USE) != 0);

        mAlert = buildAlert(context, type, personalDataMessage, checkbox).show();

        if (checkbox) {
            // Verify the last check box state to keep user's selection
            if (mIsChecked) {
                CheckBox chk = (CheckBox) mAlert.findViewById(R.id.chkbox);
                chk.setChecked(mIsChecked);
                mAlert.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            } else {
                mAlert.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }
        }
    }

    private void storeDisclamerAccepted() {
        Setting.setDisclamerAccepted(LegalDisclaimerActivity.this);
    }

    public static boolean isDisclamerAccepted(Context context) {
        return Setting.getDisclamerAccepted(context);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAlert != null) {
            mAlert.dismiss();
            mAlert = null;
        }
    }

    private void setWifiJoinInfo(Intent intent) {
        mSsid = intent.getStringExtra(StartupActivity.EXTRA_SSID);
        mPassPhrase = intent.getStringExtra(StartupActivity.EXTRA_PASS);
        mHostAddress = intent.getStringExtra(StartupActivity.EXTRA_ADDRESS);
        mSessionName = intent.getStringExtra(PartyShareActivity.EXTRA_SESSION_NAME);
        mStartTime = intent.getStringExtra(StartupActivity.EXTRA_TIME);
        mInvitedByHost = intent.getBooleanExtra(StartupActivity.EXTRA_INVITED_BY_HOST, false);
        mPermission = intent.getBooleanExtra(StartupActivity.EXTRA_PERMISSION, false);
    }

    private void goNextStep() {
        Intent intent = new Intent(this, StartupActivity.class);
        intent.putExtra(StartupActivity.EXTRA_SSID, mSsid);
        intent.putExtra(StartupActivity.EXTRA_PASS, mPassPhrase);
        intent.putExtra(StartupActivity.EXTRA_ADDRESS, mHostAddress);
        intent.putExtra(PartyShareActivity.EXTRA_SESSION_NAME, mSessionName);
        intent.putExtra(StartupActivity.EXTRA_TIME, mStartTime);
        intent.putExtra(StartupActivity.EXTRA_INVITED_BY_HOST, mInvitedByHost);
        intent.putExtra(StartupActivity.EXTRA_PERMISSION, mPermission);
        startActivity(intent);
        finish();
    }

    /**
     * Builds the disclaimer message.
     */
    private SpannableString buildTextMessage(final int type, final String personalDataMessage) {

        StringBuffer sb = new StringBuffer();

        boolean dataCharges = (type & DATA_CHARGES) != 0;
        boolean dataMessage = (type & CUSTOM_PERSONAL_DATA_MESSAGE) != 0;

        if (dataCharges) {
            sb.append(getString(R.string.disclaimer_data_charges));
        }
        if (dataMessage) {
            sb.append(personalDataMessage);
        }

        SpannableString reportingText = new SpannableString(sb);
        Linkify.addLinks(reportingText, Linkify.WEB_URLS);

        return reportingText;
    }

    /**
     * Builds the disclaimer check box message.
     */
    private Spanned buildTextMessageConcent(final int type) {

        StringBuffer sb = new StringBuffer();

        boolean personalData = (type & PERSONAL_DATA) != 0;
        boolean terms = (type & TERMS_OF_USE) != 0;

        if (personalData && terms) {
            sb.append(getString(R.string.disclaimer_terms_and_privacy_consent));
        } else if (personalData && !terms) {
            sb.append(getString(R.string.disclaimer_privacy_consent));
        } else if (!personalData && terms) {
            sb.append(getString(R.string.disclaimer_terms_consent));
        }

        Spanned reportingText = Html.fromHtml(sb.toString());

        return reportingText;
    }


    private AlertDialog.Builder buildAlert(final Context context, final int type,
            final String personalDataMessage, final boolean checkbox) {

        LayoutInflater inflater = (LayoutInflater)context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ScrollView scrollView =
                (ScrollView)inflater.inflate(R.layout.legal_disclaimer_scrollview, null);
        TextView t = (TextView)scrollView.findViewById(R.id.textView);

        t.setText(buildTextMessage(type, personalDataMessage));
        t.setMovementMethod(LinkMovementMethod.getInstance());

        TextView t2 = (TextView)scrollView.findViewById(R.id.textView2);
        t2.setText(buildTextMessageConcent(type));
        t2.setMovementMethod(LinkMovementMethod.getInstance());

        CheckBox chk = (CheckBox)scrollView.findViewById(R.id.chkbox);

        if (checkbox) {
            chk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    LegalDisclaimerActivity.this.mIsChecked = isChecked;
                    if (isChecked) {
                        mAlert.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                    } else {
                        mAlert.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    }
                }
            });
        } else {
            chk.setVisibility(View.GONE);
        }

        return new AlertDialog.Builder(context).setView(scrollView)
        .setTitle(R.string.label_terms_and_conditions)
        .setPositiveButton(R.string.label_accept, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                storeDisclamerAccepted();
                goNextStep();
            }
        }).setNegativeButton(R.string.label_decline, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }).setCancelable(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("checkbox", mIsChecked);
        outState.putString(StartupActivity.EXTRA_SSID, mSsid);
        outState.putString(StartupActivity.EXTRA_PASS, mPassPhrase);
        outState.putString(StartupActivity.EXTRA_ADDRESS, mHostAddress);
        outState.putString(PartyShareActivity.EXTRA_SESSION_NAME, mSessionName);
        outState.putString(StartupActivity.EXTRA_TIME, mStartTime);
        outState.putBoolean(StartupActivity.EXTRA_INVITED_BY_HOST, mInvitedByHost);
        outState.putBoolean(StartupActivity.EXTRA_PERMISSION, mPermission);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mIsChecked = savedInstanceState.getBoolean("checkbox");
        mSsid = savedInstanceState.getString(StartupActivity.EXTRA_SSID);
        mPassPhrase = savedInstanceState.getString(StartupActivity.EXTRA_PASS);
        mHostAddress = savedInstanceState.getString(StartupActivity.EXTRA_ADDRESS);
        mSessionName = savedInstanceState.getString(PartyShareActivity.EXTRA_SESSION_NAME);
        mStartTime = savedInstanceState.getString(StartupActivity.EXTRA_TIME);
        mInvitedByHost = savedInstanceState.getBoolean(StartupActivity.EXTRA_INVITED_BY_HOST);
        mPermission = savedInstanceState.getBoolean(StartupActivity.EXTRA_PERMISSION);
        super.onRestoreInstanceState(savedInstanceState);
    }
}
