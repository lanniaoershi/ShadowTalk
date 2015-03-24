/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.ga.TrackingUtil;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.WifiDirectStateMachine;
import com.sonymobile.partyshare.session.WifiP2pSessionInfo;
import com.sonymobile.partyshare.ui.SessionFragment.DeviceClickListener;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.Setting;

import java.util.List;

/**
 * This is session list screen. Discovers bonjour service for Wi-Fi p2p setup and
 * displays it in the list.
 */
public class PartyListActivity extends BaseActivity implements DeviceClickListener {
    private static final String FRAGMENT_TAG = "services";
    private SessionFragment mServicesFragment;
    private TextView mMyName;
    private String mStartTime = "";
    private View mCustomView;
    private AlertDialog mMyNameDialog = null;
    private AlertDialog mErrorDialog = null;
    private ProgressDialog mProgressDialog = null;

    private void partyListActivityLog(String msg) {
        LogUtil.v(LogUtil.LOG_TAG, "PartyListActivity." + msg);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        partyListActivityLog("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.partylist);

        createActionBarCustomView();

        mServicesFragment = new SessionFragment();
        getFragmentManager().beginTransaction().replace(R.id.container_root,
                mServicesFragment, FRAGMENT_TAG).commit();

        if (WifiDirectStateMachine.isConnectingState()) {
            showProgressDialog(getResources().getString(
                    R.string.party_share_strings_join_connecting_txt));
        }
    }

    @Override
    protected void onStart() {
        partyListActivityLog("onStart");
        super.onStart();
        TrackingUtil.startSession(getApplicationContext(), this);
        TrackingUtil.setScreen(getApplicationContext(), TrackingUtil.SCREEN_PARTYLIST);
        ConnectionManager.setConnectionManagerListener(this);

        updateDisplay();
    }

    @Override
    protected void onStop() {
        partyListActivityLog("onStop");
        super.onStop();
        TrackingUtil.stopSession(getApplicationContext(), this);
    }

    @Override
    public void onDestroy() {
        partyListActivityLog("onDestroy");
        super.onDestroy();
        closeProgressDialog();
        closeErrorDialog();
        ConnectionManager.removeConnectionManagerListener(this);
    }

    private void createActionBarCustomView() {
        mCustomView = getLayoutInflater().inflate(R.layout.action_bar_custom, null);
        mCustomView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showUserNameDialog();
                        }
                    });
        mMyName = (TextView) mCustomView.findViewById(R.id.user_name);
        mMyName.setText(Setting.getUserName(this));

        final ActionBar bar = getActionBar();
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                (~Gravity.VERTICAL_GRAVITY_MASK | Gravity.CENTER_VERTICAL) &
                (~Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK | Gravity.END));
        bar.setCustomView(mCustomView, lp);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME, ActionBar.DISPLAY_SHOW_HOME);
        bar.setDisplayHomeAsUpEnabled(true);

        bar.setTitle(R.string.party_share_strings_app_name_txt);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE,
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void connectP2p(WifiP2pSessionInfo sessionInfo) {
        partyListActivityLog("connectP2p");
        if (sessionInfo == null) {
            LogUtil.w(LogUtil.LOG_TAG, "sessionInfo is null.");
            return;
        }

        if (mWifiManager.isWifiEnabled()) {
            mConnectionManager.setUserName(mMyName.getText().toString());
            mPartyName = sessionInfo.sessionName;
            mStartTime = sessionInfo.time;
            closeProgressDialog();

            TrackingUtil.setJoinStartTime(System.currentTimeMillis());
            setSessionInfo(mPartyName, mStartTime);
            mStateMachine.sendMessage(WifiDirectStateMachine.CMD_CONNECT, sessionInfo);
            showProgressDialog(
                    getResources().getString(
                            R.string.party_share_strings_join_connecting_txt));
        } else {
            showWifiDisableDialog(false);
        }
    }

    private void setSessionInfo(String partyName, String startTime) {
        ConnectionManager.setSessionName(partyName);
        ConnectionManager.setStartSessionTime(startTime);
    }

    private void updateDisplay() {
        if (mConnectionManager != null) {
            mServicesFragment.updateSessionList(mConnectionManager.getSessionList());
        }
    }

    /**
     * Show user name dialog.
     */
    private void showUserNameDialog() {
        if (mMyNameDialog != null && mMyNameDialog.isShowing()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_layout_username, null);
        final EditText editText = (EditText)view.findViewById(R.id.edit);
        editText.setText(mMyName.getText().toString());
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (editText.length() > 0) {
                    mMyNameDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                } else {
                    mMyNameDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mMyNameDialog =
            builder.setView(view)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String username = editText.getText().toString();
                                    mMyName.setText(username);
                                    mConnectionManager.setUserName(username);
                                    Setting.setUserName(PartyListActivity.this, username);
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        mMyNameDialog.setCanceledOnTouchOutside(false);
        mMyNameDialog.show();
    }

    private void showProgressDialog(String msg) {
        if (mProgressDialog == null || !mProgressDialog.isShowing()) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(msg);
            mProgressDialog.setButton(
                    DialogInterface.BUTTON_NEGATIVE,
                    getResources().getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                  @Override
                  public void onCancel(DialogInterface dialog) {
                      setSessionInfo("", "");
                      mStateMachine.sendMessage(WifiDirectStateMachine.CMD_CANCEL);
                      TrackingUtil.sendJoinTime(getApplicationContext(),
                              TrackingUtil.ACTION_JOIN_CANCEL_TIME_WFD);
                  }
              });
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        } else {
            mProgressDialog.setMessage(msg);
        }
    }

    private void showErrorDialog(String msg) {
        TrackingUtil.sendEvent(getApplicationContext(), TrackingUtil.CATEGORY_JOIN,
                TrackingUtil.ACTION_JOIN_ERROR, TrackingUtil.LABEL_WIFI_DIRECT,
                TrackingUtil.VALUE_COUNT);
        TrackingUtil.iniJoinStartTime();

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_layout_textview, null);
        TextView textView = (TextView)view.findViewById(R.id.dialog_text);
        textView.setText(msg);

        if (mErrorDialog != null && mErrorDialog.isShowing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mErrorDialog = builder.setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        mErrorDialog.setCanceledOnTouchOutside(false);
        mErrorDialog.show();
    }

    private void closeProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void closeErrorDialog() {
        if (mErrorDialog != null && mErrorDialog.isShowing()) {
            mErrorDialog.dismiss();
            mErrorDialog = null;
        }
    }

    @Override
    public void updateSession(List<WifiP2pSessionInfo> sessionList) {
        super.updateSession(sessionList);
        partyListActivityLog("updateSession");
        mServicesFragment.updateSessionList(sessionList);
    }

    @Override
    public void acceptJoin() {
        super.acceptJoin();
        partyListActivityLog("acceptJoin");
        closeProgressDialog();

        if (!mForeground) {
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString(PartyShareActivity.EXTRA_SESSION_NAME,
                    ConnectionManager.getSessionName());
            intent.putExtras(bundle);
            setResult(StartupActivity.RESULT_CODE_STARTPARTY, intent);
        }
        finish();
    }

    @Override
    public void connect(String msg) {
        super.connect(msg);
        partyListActivityLog("connect");
        showProgressDialog(msg);
    }

    @Override
    public void connectError() {
        super.connectError();
        partyListActivityLog("connectError");
        closeProgressDialog();
        showErrorDialog(getResources().getString(R.string.party_share_strings_join_err_wifi_txt));
        mStateMachine.sendMessage(WifiDirectStateMachine.CMD_START_DISCOVERY);
    }

    @Override
    public void disconnected(String reason) {
        super.disconnected(reason);
        partyListActivityLog("disconnected");
        closeProgressDialog();
        if (mWifiManager.isWifiEnabled()) {
            if (reason != null) {
                showErrorDialog(reason);
            }
            mServicesFragment.clearSessionList();
            mStateMachine.sendMessage(WifiDirectStateMachine.CMD_START_DISCOVERY);
        } else {
            showWifiDisableDialog(false);
        }
    }
}
