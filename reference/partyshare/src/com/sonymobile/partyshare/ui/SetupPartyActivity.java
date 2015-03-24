/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.ga.TrackingUtil;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.WifiDirectStateMachine;
import com.sonymobile.partyshare.util.Setting;

public class SetupPartyActivity extends BaseActivity implements View.OnClickListener {
    private EditText mSessionEdit;
    private EditText mUserEdit;
    private Button mStartButton;
    private AlertDialog mConfirmationDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_party);

        mStartButton = (Button)findViewById(R.id.btn_host);
        mStartButton.setOnClickListener(this);

        mSessionEdit = (EditText)findViewById(R.id.edit_party_name);
        mUserEdit = (EditText)findViewById(R.id.edit_user_name);

        String userName = Setting.getUserName(this);
        String defaultPartyName = String.format(getResources().getString(
                R.string.party_share_strings_party_name_default_txt), userName);
        mSessionEdit.setText(defaultPartyName);
        mUserEdit.setText(Setting.getUserName(this));

        if (userName.isEmpty() || defaultPartyName.isEmpty()) {
            mStartButton.setEnabled(false);
        } else {
            mStartButton.setEnabled(true);
        }

        mSessionEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (mSessionEdit.length() > 0 && mUserEdit.length() > 0) {
                    mStartButton.setEnabled(true);
                } else {
                    mStartButton.setEnabled(false);
                }
            }
        });

        mUserEdit.addTextChangedListener(new TextWatcher() {
            private boolean mAutoChange = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                String partyStr = mSessionEdit.getText().toString();
                String userStr = mUserEdit.getText().toString();
                String defaultPartyStr = String.format(getResources().getString(
                        R.string.party_share_strings_party_name_default_txt), userStr);
                if (!partyStr.isEmpty() && partyStr.equals(defaultPartyStr)) {
                    mAutoChange = true;
                } else {
                    mAutoChange = false;
                }
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (mSessionEdit.length() > 0 && mUserEdit.length() > 0) {
                    mStartButton.setEnabled(true);
                } else {
                    mStartButton.setEnabled(false);
                }
                if (mAutoChange) {
                    String defaultPartyStr = String.format(getResources().getString(
                            R.string.party_share_strings_party_name_default_txt), s.toString());
                    mSessionEdit.setText(defaultPartyStr);
                }
            }
        });
        createActionBar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        TrackingUtil.startSession(getApplicationContext(), this);
        TrackingUtil.setScreen(getApplicationContext(), TrackingUtil.SCREEN_SETTINGS);
        ConnectionManager.setConnectionManagerListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        TrackingUtil.stopSession(getApplicationContext(), this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mConfirmationDialog != null && mConfirmationDialog.isShowing()) {
            mConfirmationDialog.dismiss();
            mConfirmationDialog = null;
        }
        ConnectionManager.removeConnectionManagerListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            setResult(Activity.RESULT_CANCELED);
            finish();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void createActionBar() {
        final ActionBar bar = getActionBar();
        bar.setTitle(R.string.party_share_strings_app_name_txt);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE,
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        bar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void startSession(boolean result) {
        if (result) {
            startParty();
        } else {
            mStartButton.setEnabled(true);
        }
    }

    private void startParty() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(PartyShareActivity.EXTRA_SESSION_NAME, mSessionEdit.getText().toString());
        intent.putExtras(bundle);
        setResult(StartupActivity.RESULT_CODE_STARTPARTY, intent);
        finish();
    }

    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        mStartButton.setEnabled(false);
        mConfirmationDialog =
                builder.setMessage(R.string.party_share_strings_nfc_permission_msg_txt)
                    .setNegativeButton(R.string.party_share_strings_dialog_no_button_txt,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialogButtonAction(false);
                                }
                            })
                    .setPositiveButton(R.string.party_share_strings_dialog_yes_button_txt,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialogButtonAction(true);
                                }
                            })
                    .create();
        mConfirmationDialog.setCanceledOnTouchOutside(false);
        mConfirmationDialog.setCancelable(true);
        mConfirmationDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mStartButton.setEnabled(true);
            }
        });
        mConfirmationDialog.show();
    }
    private void dialogButtonAction(boolean permission) {
        ConnectionManager.setNfcJoinPermission(permission);
        ConnectionManager.setSessionName(mSessionEdit.getText().toString());
        mStateMachine.sendMessage(WifiDirectStateMachine.CMD_START_SESSION);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
        case R.id.btn_host:
            if (mWifiManager.isWifiEnabled()) {
                if (mSessionEdit.getText().toString().isEmpty() ||
                        mUserEdit.getText().toString().isEmpty()) {
                    return;
                }
                Setting.setUserName(SetupPartyActivity.this, mUserEdit.getText().toString());
                showConfirmationDialog();
            } else {
                showWifiDisableDialog(false);
            }
            break;
        default:
            break;
        }
    }
}
