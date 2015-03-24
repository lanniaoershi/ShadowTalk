/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;

import com.sonymobile.partyshare.ga.TrackingUtil;
import com.sonymobile.partyshare.session.WifiDirectStateMachine;
import com.sonymobile.partyshare.util.LogUtil;

import java.nio.charset.Charset;

/**
 * This activity receives NDEF_DISCOVERED intent.
 * Then it launches StartupActivity.
 */
public class NfcEntry extends Activity {

    /**
     * onCreate.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Parcelable[] msgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (msgs == null) {
            LogUtil.e(LogUtil.LOG_TAG, "NfcEntry msg error.");
            finish();
            return;
        }

        NdefMessage msg = (NdefMessage)msgs[0];
        if (msg.getRecords().length < 7 || msg.getRecords()[0] == null ||
            msg.getRecords()[1] == null || msg.getRecords()[2] == null ||
            msg.getRecords()[3] == null || msg.getRecords()[4] == null ||
            msg.getRecords()[5] == null || msg.getRecords()[6] == null) {
            LogUtil.e(LogUtil.LOG_TAG, "NfcEntry record error.");
            finish();
            return;
        }

        TrackingUtil.setJoinStartTime(System.currentTimeMillis());

        Intent intent = new Intent();
        intent.setClass(this, StartupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        String ssid = new String(msg.getRecords()[0].getPayload(), Charset.forName("UTF-8"));
        String passPhrase = new String(msg.getRecords()[1].getPayload(), Charset.forName("UTF-8"));
        String address = new String(msg.getRecords()[2].getPayload(), Charset.forName("UTF-8"));
        String session = new String(msg.getRecords()[3].getPayload(), Charset.forName("UTF-8"));
        String time = new String(msg.getRecords()[4].getPayload(), Charset.forName("UTF-8"));
        boolean invitedByHost = Boolean.valueOf(new String(msg.getRecords()[5].getPayload(),
                                            Charset.forName("UTF-8"))).booleanValue();
        boolean permission = Boolean.valueOf(new String(msg.getRecords()[6].getPayload(),
                                            Charset.forName("UTF-8"))).booleanValue();

        // if app is not joined, set owner wifi info.
        if (!WifiDirectStateMachine.isJoiningSession()) {
            intent.putExtra(StartupActivity.EXTRA_SSID, ssid);
            intent.putExtra(StartupActivity.EXTRA_PASS, passPhrase);
            intent.putExtra(StartupActivity.EXTRA_ADDRESS, address);
            intent.putExtra(PartyShareActivity.EXTRA_SESSION_NAME, session);
            intent.putExtra(StartupActivity.EXTRA_TIME, time);
            intent.putExtra(StartupActivity.EXTRA_INVITED_BY_HOST, invitedByHost);
            intent.putExtra(StartupActivity.EXTRA_PERMISSION, permission);
        }
        startActivity(intent);
        finish();
    }
}
