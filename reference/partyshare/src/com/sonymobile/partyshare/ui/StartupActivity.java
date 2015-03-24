/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.ga.TrackingUtil;
import com.sonymobile.partyshare.service.PartyShareService;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.WifiDirectStateMachine;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.Setting;
import com.sonymobile.partyshare.util.Utility;


/**
 * This is party share app entry.
 */
public class StartupActivity extends BaseActivity implements View.OnClickListener {
    public static final int REQUEST_CODE_STARTPARTY = 1;
    public static final int RESULT_CODE_STARTPARTY = 1;

    public static final String EXTRA_SSID = "extra_ssid";
    public static final String EXTRA_PASS = "extra_pass";
    public static final String EXTRA_ADDRESS = "extra_address";
    public static final String EXTRA_TIME = "extra_time";
    public static final String EXTRA_INVITED_BY_HOST = "extra_invited_by_host";
    public static final String EXTRA_PERMISSION = "extra_permission";
    private static final int MARGIN_TOP_DP = 4;

    private static boolean sIsAppStarted = false;

    private String mSsid;
    private String mPassPhrase;
    private String mHostAddress;
    private String mStartTime = "";
    private boolean mInvitedByHost = false;
    private boolean mPermission = false;
    private ProgressDialog mWifiProgressDialog;
    private boolean mIsWifiConnect = false;
    private AlertDialog mHelpDialog = null;
    private boolean mNfcFlg = false;

    private AlertDialog mSecurityAlertDialog;

    private static void setAppStartFlag(boolean start) {
        sIsAppStarted = start;
    }

    // Wifi connecting task.
    private class WifiConnector extends WifiConnectTask {
        @Override
        protected void onPostExecute(Integer result) {
            mWifiConnector = null;
            mIsWifiConnect = false;
            if (result != null && WifiConnectTask.RESULT_SUCCEEDED == result) {
                Toast.makeText(StartupActivity.this,
                    R.string.party_share_strings_wifi_success_txt, Toast.LENGTH_LONG).show();
                ConnectionManager.setSsid(mSsid);
                ConnectionManager.setPassPhrase(mPassPhrase);
                ConnectionManager.setGroupOwnerAddress(mHostAddress);
                ConnectionManager.setNfcJoinPermission(mPermission);

                mStateMachine.sendMessage(
                        WifiDirectStateMachine.CMD_WIFI_CONNECT_SUCCESS,
                        mHostAddress);
            } else {
                LogUtil.v(LogUtil.LOG_TAG, "WifiConnector.onPostExecute : " + result);
                mStateMachine.sendMessage(WifiDirectStateMachine.CMD_CANCEL);
                Toast.makeText(StartupActivity.this,
                    R.string.party_share_strings_join_err_wifi_txt, Toast.LENGTH_LONG).show();
                mNfcFlg = false;
                TrackingUtil.sendEvent(getApplicationContext(), TrackingUtil.CATEGORY_JOIN,
                        TrackingUtil.ACTION_JOIN_ERROR, TrackingUtil.LABEL_NFC,
                        TrackingUtil.VALUE_COUNT);
                TrackingUtil.iniJoinStartTime();
            }
            mWifiProgressDialog.dismiss();
        }
    }
    private WifiConnectTask mWifiConnector = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogUtil.v(LogUtil.LOG_TAG, "StartupActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup);

        if (Utility.excludeCheck(this)) {
            startWarningActivity(WarningActivity.WARNING_KIND_UPDATE);
            return;
        }

        if (!isOwner()) {
            startWarningActivity(WarningActivity.WARNING_KIND_NOT_OWNER);
            return;
        }

        mSsid = getIntent().getStringExtra(EXTRA_SSID);
        mPassPhrase = getIntent().getStringExtra(EXTRA_PASS);
        mHostAddress = getIntent().getStringExtra(EXTRA_ADDRESS);
        mPartyName = getIntent().getStringExtra(PartyShareActivity.EXTRA_SESSION_NAME);
        mStartTime = getIntent().getStringExtra(EXTRA_TIME);
        mInvitedByHost = getIntent().getBooleanExtra(EXTRA_INVITED_BY_HOST, false);
        mPermission = getIntent().getBooleanExtra(EXTRA_PERMISSION, false);

        mIsWifiConnect = mSsid != null && !mSsid.isEmpty() && mPassPhrase != null &&
                !mPassPhrase.isEmpty() && mHostAddress != null && !mHostAddress.isEmpty();

        if (!LegalDisclaimerActivity.isDisclamerAccepted(this)) {
            startLegalDisclaimerActivity();
            return;
        }

        initView();

        if (Setting.getDisplaySecurityAlert(getApplicationContext())) {
            if (!sIsAppStarted) {
                showSecurityAlertDialog();
            } else {
                startService();
            }
        } else {
            startService();
        }
    }

    @Override
    public void onStart() {
        LogUtil.v(LogUtil.LOG_TAG, "StartupActivity.onStart");
        super.onStart();
        TrackingUtil.startSession(getApplicationContext(), this);
        TrackingUtil.setScreen(getApplicationContext(), TrackingUtil.SCREEN_MAINSCREEN);
        ConnectionManager.setConnectionManagerListener(this);
        if (mWifiConnector != null && mWifiConnector.getStatus() == AsyncTask.Status.RUNNING) {
            showWifiProgressDialog();
        }
    }

    @Override
    public void onResume() {
        LogUtil.v(LogUtil.LOG_TAG, "StartupActivity.onResume");
        super.onResume();
        stateCheck();
    }

    @Override
    public void onStop() {
        LogUtil.v(LogUtil.LOG_TAG, "StartupActivity.onStop");
        super.onStop();
        TrackingUtil.stopSession(getApplicationContext(), this);
        if (mWifiProgressDialog != null && mWifiProgressDialog.isShowing()) {
            mWifiProgressDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        LogUtil.v(LogUtil.LOG_TAG, "StartupActivity.onDestroy");
        super.onDestroy();
        if (mSecurityAlertDialog != null && mSecurityAlertDialog.isShowing()) {
            mSecurityAlertDialog.dismiss();
            mSecurityAlertDialog = null;
        }

        ConnectionManager.removeConnectionManagerListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        LogUtil.v(LogUtil.LOG_TAG, "StartupActivity.onNewIntent()");
        super.onNewIntent(intent);

        if (mWifiConnector != null && mWifiConnector.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }
        mSsid = intent.getStringExtra(EXTRA_SSID);
        mPassPhrase = intent.getStringExtra(EXTRA_PASS);
        mHostAddress = intent.getStringExtra(EXTRA_ADDRESS);
        mPartyName = intent.getStringExtra(PartyShareActivity.EXTRA_SESSION_NAME);
        mStartTime = intent.getStringExtra(EXTRA_TIME);
        mInvitedByHost = intent.getBooleanExtra(EXTRA_INVITED_BY_HOST, false);
        mPermission = intent.getBooleanExtra(EXTRA_PERMISSION, false);

        mIsWifiConnect = mSsid != null && !mSsid.isEmpty() && mPassPhrase != null &&
                !mPassPhrase.isEmpty() && mHostAddress != null && !mHostAddress.isEmpty();

        if (mIsWifiConnect) {
            joinSessionViaWifi();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_help:
            pageScrollTo();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        setAppStartFlag(false);
        stopService(new Intent(this, PartyShareService.class));
        super.onBackPressed();
    }

    private void initView() {
        LogUtil.v(LogUtil.LOG_TAG, "StartupActivity.initView()");

        ScrollView scv = (ScrollView)findViewById(R.id.help_layout);
        TextView moreTxt = (TextView)scv.findViewById(R.id.show_more);
        moreTxt.setOnClickListener(this);
        SpannableString content = new SpannableString(moreTxt.getText());
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        moreTxt.setText(content);
        Button sendBtn = (Button)scv.findViewById(R.id.btn_send);
        sendBtn.setOnClickListener(this);

        PackageManager pm = getPackageManager();
        ResolveInfo resolveInfo =
                pm.resolveActivity(createSendIntent(), PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo == null) {
            sendBtn.setEnabled(false);
        }

        Button newPartyBtn = (Button)findViewById(R.id.btn_new_party);
        newPartyBtn.setOnClickListener(this);
        if (!Utility.hasHostCapability(this)) {
            newPartyBtn.setVisibility(View.GONE);
            ((View)findViewById(R.id.divider)).setVisibility(View.GONE);
        }

        Button joinBtn = (Button)findViewById(R.id.btn_guest);
        joinBtn.setOnClickListener(this);

        createActionBar();
    }

    private void startService() {
        Intent intent = new Intent(this, PartyShareService.class);
        intent.setAction(PartyShareService.ACTION_START);
        startService(intent);

        mStateMachine.sendMessage(WifiDirectStateMachine.CMD_INITIALIZE);

        if (!WifiDirectStateMachine.isJoiningSession()) {
            LogUtil.v(LogUtil.LOG_TAG, "initialize start");
            mConnectionManager.initialize();
            mConnectionManager.start();
        }

        if (mIsWifiConnect) {
            joinSessionViaWifi();
        }
    }

    private void createActionBar() {
        final ActionBar bar = getActionBar();
        bar.setTitle(R.string.party_share_strings_app_name_txt);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE,
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
    }

    private Intent createSendIntent() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.url));
        return intent;
    }

    private void showSecurityAlertDialog() {
        LayoutInflater inflater = (LayoutInflater)this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ScrollView scrollView = (ScrollView)inflater.inflate(R.layout.security_alert, null);

        CheckBox chk = (CheckBox)scrollView.findViewById(R.id.chkbox);

        chk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Setting.setDisplaySecurityAlert(getApplicationContext(), !isChecked);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mSecurityAlertDialog = builder.setView(scrollView)
                .setTitle(android.R.string.dialog_alert_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setAppStartFlag(true);
                        startService();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Setting.setDisplaySecurityAlert(getApplicationContext(), true);
                        setAppStartFlag(false);
                        mConnectionManager.terminate();
                        ConnectionManager.destroy();
                        finish();
                    }
                })
                .create();
        mSecurityAlertDialog.setCancelable(false);
        mSecurityAlertDialog.show();
    }

    private void stateCheck() {
        if (WifiDirectStateMachine.isOnGoingSession() ||
                WifiDirectStateMachine.isConnectedState()) {
            Intent intent = new Intent();
            intent.setClass(this, PartyShareActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(PartyShareActivity.EXTRA_ALREADY_STARTED, true);
            intent.putExtra(PartyShareActivity.EXTRA_SESSION_NAME,
                    ConnectionManager.getSessionName());
            startActivity(intent);
            finish();
        } else if (WifiDirectStateMachine.isConnectingState()) {
            if (mWifiManager.isWifiEnabled()) {
                Intent intent = new Intent(this, PartyListActivity.class);
                startActivityForResult(intent, REQUEST_CODE_STARTPARTY);
            } else {
                showWifiDisableDialog(true);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
        case REQUEST_CODE_STARTPARTY:
            if (resultCode == RESULT_CODE_STARTPARTY) {
                Bundle bundle = intent.getExtras();
                String sessionName = bundle.getString(PartyShareActivity.EXTRA_SESSION_NAME);
                startParty(sessionName);
            }
            break;
        default:
            break;
        }
    }

    private void startLegalDisclaimerActivity() {
        Intent intent = new Intent(this, LegalDisclaimerActivity.class);

        // Select an appropriate disclaimer message
        int disclamermask = LegalDisclaimerActivity.TERMS_OF_USE;

        // Put the selection bit-mask in the intent
        intent.putExtra(LegalDisclaimerActivity.DISCLAMER_SELECTION_EXTRA, disclamermask);

        // Add application specific message about collection of personal data,
        // this should be translated.
        intent.putExtra(LegalDisclaimerActivity.DISCLAMER_CUSTOM_PERSONAL_DATA_TEXT_EXTRA,
                "Sony Mobile will collect you telephone identification number (IMEI) and "
                + "location based data in order to provide you with personal recommendations.");

        // set wifi join info
        intent.putExtra(EXTRA_SSID, mSsid);
        intent.putExtra(EXTRA_PASS, mPassPhrase);
        intent.putExtra(EXTRA_ADDRESS, mHostAddress);
        intent.putExtra(PartyShareActivity.EXTRA_SESSION_NAME, mPartyName);
        intent.putExtra(EXTRA_TIME, mStartTime);
        intent.putExtra(EXTRA_INVITED_BY_HOST, mInvitedByHost);
        intent.putExtra(EXTRA_PERMISSION, mPermission);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(intent);
        finish();
    }

    private void startWarningActivity(int kind) {
        Intent intent = new Intent(this, WarningActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(WarningActivity.EXTRA_KIND, kind);
        startActivity(intent);
        finish();
    }

    private boolean isOwner() {
        boolean isOwner = true;
        if (Process.myUserHandle().hashCode() != 0) {
            isOwner = false;
        }
        return isOwner;
    }

    private void startParty(String sessionName) {
        ConnectionManager.setSessionName(sessionName);
        Intent intent = new Intent();
        intent.setClass(this, PartyShareActivity.class);
        intent.putExtra(PartyShareActivity.EXTRA_SESSION_NAME, sessionName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        TrackingUtil.iniMaxJoinMembers();
        TrackingUtil.setPartyStartTime(System.currentTimeMillis());
        finish();
    }

    private void joinSessionViaWifi() {
        LogUtil.v(LogUtil.LOG_TAG, "StartupActivity.joinSessionViaWifi - SSID : " + mSsid +
                ", PassPhrase : " + mPassPhrase + ", HostAddress : " + mHostAddress +
                ", host : " + mInvitedByHost + ", permission : " + mPermission);
        if (!mInvitedByHost && !mPermission) {
            showInvitationRestrictedDialog();
        } else {
            mStateMachine.sendMessage(WifiDirectStateMachine.CMD_WIFI_CONNECT);
            mNfcFlg = true;
            WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
            mWifiConnector = new WifiConnector();
            mWifiConnector.execute(wifiManager, mSsid, mPassPhrase);
            setSessionInfo(mPartyName, mStartTime);
            showWifiProgressDialog();
        }
    }

    private void showWifiProgressDialog() {
        if (mWifiProgressDialog == null || !mWifiProgressDialog.isShowing()) {
            mWifiProgressDialog = new ProgressDialog(this);
            mWifiProgressDialog.setMessage(
                    getResources().getString(R.string.party_share_strings_wifi_connecting_txt));
            mWifiProgressDialog.setButton(
                    DialogInterface.BUTTON_NEGATIVE,
                    getResources().getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            mWifiProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        setSessionInfo("", "");
                        mStateMachine.sendMessage(WifiDirectStateMachine.CMD_CANCEL);
                        if (mWifiConnector != null && !mWifiConnector.isCancelled()) {
                            mWifiConnector.cancel(true);
                            mWifiConnector = null;
                        }
                        mIsWifiConnect = false;
                        LogUtil.d(LogUtil.LOG_TAG, "Wi-Fi connecting is cancelled.");
                        mNfcFlg = false;
                        TrackingUtil.sendJoinTime(getApplicationContext(),
                                TrackingUtil.ACTION_JOIN_CANCEL_TIME_NFC);
                    }
            });
            mWifiProgressDialog.setCanceledOnTouchOutside(false);
            mWifiProgressDialog.show();
        }
    }

    private void setSessionInfo(String partyName, String startTime) {
        LogUtil.v(LogUtil.LOG_TAG, "StartupActivity.setSessionInfo");
        ConnectionManager.setSessionName(partyName);
        ConnectionManager.setStartSessionTime(startTime);
    }

    private void showHelpDetailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mHelpDialog =
            builder.setMessage(
                    getResources().getString(R.string.party_share_strings_help_nfc_detail_txt))
                    .setPositiveButton(android.R.string.ok, null)
                 .create();
        mHelpDialog.setCanceledOnTouchOutside(false);
        mHelpDialog.show();
    }

    private void pageScrollTo() {
        final TypedValue tv = new TypedValue();
        float density = getResources().getDisplayMetrics().density;
        int actionBarHeight = 0;
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(
                    tv.data, getResources().getDisplayMetrics());
        }
        final int marginTop = actionBarHeight + (int)(MARGIN_TOP_DP * density + 0.5f);
        LogUtil.d(LogUtil.LOG_TAG, "pageScrollTo marginTop : " + marginTop);
        final ScrollView scv = (ScrollView)findViewById(R.id.help_layout);
        final View startTitleView = scv.findViewById(R.id.help_start_title);

        scv.post(new Runnable() {
            public void run() {
                int targetY = (int)startTitleView.getY();
                int scrollY = targetY - marginTop > 0 ? targetY - marginTop : 0;
                LogUtil.d(LogUtil.LOG_TAG, "pageScrollTo scrollY : " + scrollY);
                scv.scrollTo(0, scrollY);
            }
        });
    }

    @Override
    public void acceptJoin() {
        super.acceptJoin();
        LogUtil.v(LogUtil.LOG_TAG, "StartupActivity.acceptJoin");

        if (mNfcFlg) {
            TrackingUtil.sendEvent(getApplicationContext(), TrackingUtil.CATEGORY_JOIN,
                    TrackingUtil.ACTION_HOW_TO_JOIN, TrackingUtil.LABEL_NFC,
                    TrackingUtil.VALUE_COUNT);
            TrackingUtil.sendJoinTime(getApplicationContext(),
                    TrackingUtil.ACTION_JOIN_TIME_NFC);
        } else {
            TrackingUtil.sendEvent(getApplicationContext(), TrackingUtil.CATEGORY_JOIN,
                    TrackingUtil.ACTION_HOW_TO_JOIN, TrackingUtil.LABEL_WIFI_DIRECT,
                    TrackingUtil.VALUE_COUNT);
            TrackingUtil.sendJoinTime(getApplicationContext(),
                    TrackingUtil.ACTION_JOIN_TIME_WFD);
        }
        if (!mForeground) {
            Intent intent = new Intent();
            intent.setClass(this, PartyShareActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (mPartyName != null) {
                intent.putExtra(PartyShareActivity.EXTRA_SESSION_NAME, mPartyName);
            } else {
                intent.putExtra(PartyShareActivity.EXTRA_SESSION_NAME,
                        ConnectionManager.getSessionName());
            }
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void disconnected(String reason) {
        super.disconnected(reason);
        LogUtil.v(LogUtil.LOG_TAG, "StartupActivity.disconnected");
        if (mWifiProgressDialog != null && mWifiProgressDialog.isShowing()) {
            mWifiProgressDialog.cancel();
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch(v.getId()) {
        case R.id.btn_new_party:
            if (mWifiManager.isWifiEnabled()) {
                intent = new Intent(this, SetupPartyActivity.class);
                startActivityForResult(intent, REQUEST_CODE_STARTPARTY);
            } else {
                showWifiDisableDialog(true);
            }
            break;
        case R.id.btn_guest:
            if (mWifiManager.isWifiEnabled()) {
                intent = new Intent(this, PartyListActivity.class);
                startActivityForResult(intent, REQUEST_CODE_STARTPARTY);
            } else {
                showWifiDisableDialog(true);
            }
            break;
        case R.id.btn_send:
            intent = createSendIntent();
            try {
               startActivity(intent);
            } catch (ActivityNotFoundException e) {
                LogUtil.e(LogUtil.LOG_TAG, "ActivityNotFoundException : " + e.toString());
            }
            break;
        case R.id.show_more:
            showHelpDetailDialog();
            break;
        default:
            break;
        }
    }
}
