/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.ga.TrackingUtil;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.WifiDirectStateMachine;
import com.sonymobile.partyshare.util.LogUtil;


/**
 * Display about legal.
 */
public class LegalActivity extends BaseActivity {

    private boolean mJoinParty;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legal_view);
        TextView mainText = (TextView) findViewById(R.id.detail_main_text);
        mainText.setSingleLine(false);
        mainText.setText((Html.fromHtml(getResources().getString(R.string.license_text))));
        mainText.setMovementMethod(LinkMovementMethod.getInstance());
        createActionBar();

        if (WifiDirectStateMachine.isOnGoingSession() ||
                WifiDirectStateMachine.isConnectedState()) {
            mJoinParty = true;
        } else {
            mJoinParty = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        TrackingUtil.startSession(getApplicationContext(), this);
        TrackingUtil.setScreen(getApplicationContext(), TrackingUtil.SCREEN_LEGAL);
        ConnectionManager.setConnectionManagerListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        TrackingUtil.stopSession(getApplicationContext(), this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ConnectionManager.removeConnectionManagerListener(this);
    }

    @Override
    public void disconnected(String reason) {
        super.disconnected(reason);
        LogUtil.v(LogUtil.LOG_TAG, "HelpActivity.disconnected");

        if (!mForeground && mJoinParty) {
            Intent intent = new Intent();
            intent.setClass(this, StartupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return false;
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

    private void createActionBar() {
        final ActionBar bar = getActionBar();
        bar.setTitle(R.string.party_share_strings_option_menu_legal_txt);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE,
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        bar.setDisplayHomeAsUpEnabled(true);
    }
}
