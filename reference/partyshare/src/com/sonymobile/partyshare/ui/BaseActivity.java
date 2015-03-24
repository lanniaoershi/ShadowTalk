/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.ConnectionManager.ConnectionManagerListener;
import com.sonymobile.partyshare.session.DeviceInfo;
import com.sonymobile.partyshare.session.WifiDirectStateMachine;
import com.sonymobile.partyshare.session.WifiP2pSessionInfo;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.Utility;


/**
 * This is base activity class. It has common processes.
 */
public abstract class BaseActivity extends FragmentActivity implements
        ConnectionManagerListener, CreateNdefMessageCallback, OnNdefPushCompleteCallback {

    protected WifiDirectStateMachine mStateMachine = null;
    protected ConnectionManager mConnectionManager = null;

    protected String mUserName = "";
    protected String mPartyName = "";
    protected boolean mForeground = false;
    protected WifiManager mWifiManager;

    private static boolean sShowRestrictedDialog = false;
    private AlertDialog mInvitationRestrictedDialog;
    private AlertDialog mWifiOffErrorDialog = null;
    private NfcAdapter mNfcAdapter;
    private Handler mHandler = new Handler();

    private static void setRestricted(boolean isShown) {
        sShowRestrictedDialog = isShown;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogUtil.v(LogUtil.LOG_TAG, "BaseActivity.onCreate");
        super.onCreate(savedInstanceState);
        mWifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        mConnectionManager = ConnectionManager.getInstance(this.getApplicationContext());
        mStateMachine = mConnectionManager.getStateMachine();

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            LogUtil.e(LogUtil.LOG_TAG, "NFC is not supported.");
        } else {
            if (!mNfcAdapter.isEnabled()) {
                //startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                //Toast.makeText(this, "NFC is disabled", Toast.LENGTH_LONG).show();
            }
            mNfcAdapter.setBeamPushUrisCallback(null, this);
            mNfcAdapter.setNdefPushMessageCallback(this, this);
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }
    }

    @Override
    public void onResume() {
        LogUtil.v(LogUtil.LOG_TAG, "BaseActivity.onResume");
        super.onResume();
        wifiOn();
    }

    @Override
    protected void onStart() {
        LogUtil.v(LogUtil.LOG_TAG, "BaseActivity.onStart");
        super.onStart();
        mForeground = false;
    }

    @Override
    protected void onStop() {
        LogUtil.v(LogUtil.LOG_TAG, "BaseActivity.onStop");
        super.onStop();
        mForeground = true;
    }

    @Override
    public void onDestroy() {
        LogUtil.v(LogUtil.LOG_TAG, "BaseActivity.onDestroy");
        if (mWifiOffErrorDialog != null && mWifiOffErrorDialog.isShowing()) {
            mWifiOffErrorDialog.dismiss();
            mWifiOffErrorDialog = null;
        }
        if (mInvitationRestrictedDialog != null && mInvitationRestrictedDialog.isShowing()) {
            mInvitationRestrictedDialog.dismiss();
            mInvitationRestrictedDialog = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_items_base, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
        case R.id.menu_download_mode:
            new DownloadModeDialog(this).createDialog().show();
            break;
        case R.id.menu_sort_photo:
            new SortOrderPhotoDialog(this).createDialog().show();
            break;
        case R.id.menu_legal:
            intent = new Intent(this, LegalActivity.class);
            startActivity(intent);
            break;
        case R.id.menu_help:
            intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean isForeground() {
        boolean forcus = hasWindowFocus();
        LogUtil.v(LogUtil.LOG_TAG, "BaseActivity.isForeground:" + forcus);
        return forcus;
    }

    public void wifiOn() {
        if(!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
            Utility.showToast(
                    this,
                    getResources().getString(R.string.party_share_strings_wifi_on_txt),
                    Toast.LENGTH_LONG);
        }
    }

    public void showWifiDisableDialog(boolean top) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_layout_textview, null);
        TextView textView = (TextView)view.findViewById(R.id.dialog_text);
        textView.setText(getResources().getText(R.string.party_share_strings_wifi_off_err_txt));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mWifiOffErrorDialog = builder.setView(view)
                .setPositiveButton(R.string.party_share_strings_dialog_close_button_txt, null)
                .create();
        if (!top) {
            mWifiOffErrorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    Intent intent = new Intent();
                    intent.setClass(BaseActivity.this, StartupActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            });
        } else {
            mWifiOffErrorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    wifiOn();
                }
            });
        }
        mWifiOffErrorDialog.setCanceledOnTouchOutside(false);
        mWifiOffErrorDialog.show();
    }

    @Override
    public void acceptJoin() {}

    @Override
    public void updateMemberList(List<DeviceInfo> groupList) {}

    @Override
    public void updateSession(List<WifiP2pSessionInfo> sessionList) {}

    @Override
    public void disconnected(String reason) {}

    @Override
    public void connectError() {}

    @Override
    public void connect(String msg) {}

    @Override
    public void startSession(boolean result) {}

    // CreateNdefMessageCallback method
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        boolean isHost = ConnectionManager.isGroupOwner();
        boolean isAllowed = ConnectionManager.getNfcJoinPermission();
        NdefMessage msg = Utility.createNdefMsg(ConnectionManager.getSsid(),
                ConnectionManager.getPassPhrase(),
                ConnectionManager.getGroupOwnerAddress(),
                ConnectionManager.getSessionName(),
                ConnectionManager.getStartSessionTime(),
                isHost,
                isAllowed);
        LogUtil.v(LogUtil.LOG_TAG,
                "createNdefMessage isHost : " + isHost + ", isAllowed : " + isAllowed);
        if (msg != null && msg.getRecords().length > 1 && !isHost && !isAllowed) {
            setRestricted(true);
        } else {
            setRestricted(false);
        }
        return msg;
    }

    // OnNdefPushCompleteCallback method
    @Override
    public void onNdefPushComplete(NfcEvent arg0) {
        LogUtil.v(LogUtil.LOG_TAG, "onNdefPushComplete : " + sShowRestrictedDialog);
        if (sShowRestrictedDialog) {
            mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showInvitationRestrictedDialog();
                    }
            });
        }
    }

    protected void showInvitationRestrictedDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_layout_textview, null);
        TextView textView = (TextView)view.findViewById(R.id.dialog_text);
        textView.setText(getResources().getText(R.string.party_share_strings_nfc_prohibit_msg_txt));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mInvitationRestrictedDialog = builder.setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        mInvitationRestrictedDialog.setCanceledOnTouchOutside(false);
        mInvitationRestrictedDialog.show();
    }
}
