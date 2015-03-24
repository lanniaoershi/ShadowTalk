/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.ga.TrackingUtil;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.WifiDirectStateMachine;
import com.sonymobile.partyshare.util.LogUtil;

/**
 * Help screen. Display a description of how to play, how to invite, etc.
 */
public class HelpActivity extends BaseActivity implements View.OnClickListener {
    public static final String EXTRA_SHOW_TOP = "extra_show_top";
    private static final int MARGIN_TOP_DP = 12;
    private AlertDialog mHelpDialog = null;

    private boolean mJoinParty;

    /**
     * onCreate.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.help);
        TextView moreText = (TextView)findViewById(R.id.show_more);
        moreText.setOnClickListener(this);
        SpannableString content = new SpannableString(moreText.getText());
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        moreText.setText(content);
        Button sendBtn = (Button)findViewById(R.id.btn_send);
        sendBtn.setOnClickListener(this);
        PackageManager pm = getPackageManager();
        ResolveInfo resolveInfo =
                pm.resolveActivity(createSendIntent(), PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo == null) {
            sendBtn.setEnabled(false);
        }

        createActionBar();
        if (!getIntent().getBooleanExtra(EXTRA_SHOW_TOP, true)) {
            pageScrollTo();
        }

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
        TrackingUtil.setScreen(getApplicationContext(), TrackingUtil.SCREEN_HELP);
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
        ConnectionManager.removeConnectionManagerListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem shareItem = menu.findItem(R.id.menu_help);
        shareItem.setVisible(false);
        return true;
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

    /**
     * onClick.
     * @param v View
     */
    @Override
    public void onClick(View v) {
        switch(v.getId()) {
        case R.id.btn_send:
            Intent intent = createSendIntent();
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

    private void createActionBar() {
        final ActionBar bar = getActionBar();
        bar.setTitle(R.string.party_share_strings_app_name_txt);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE,
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        bar.setDisplayHomeAsUpEnabled(true);
    }

    private Intent createSendIntent() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.url));
        return intent;
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

        final ScrollView scv = (ScrollView)findViewById(R.id.help_scroll_view);
        final View nfcTitleView = findViewById(R.id.btn_send);

        scv.post(new Runnable() {
            public void run() {
                int targetY = (int)nfcTitleView.getY();
                int scrollY = targetY - marginTop > 0 ? targetY - marginTop : 0;
                LogUtil.d(LogUtil.LOG_TAG, "pageScrollTo scrollY : " + scrollY);
                scv.scrollTo(0, scrollY);
            }
        });
    }
}
