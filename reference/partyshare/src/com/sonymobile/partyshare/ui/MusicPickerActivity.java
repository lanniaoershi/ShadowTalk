/*
 * Copyright (c) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.ActionBar;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.ga.TrackingUtil;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.util.LogUtil;

import java.lang.Object;
import java.util.ArrayList;

/*
 * This is music picker. The music list which is retrieved from media DB is displayed.
 */
public class MusicPickerActivity extends BaseActivity implements View.OnClickListener {
    private static final String[] PROJECTION = new String[] {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
    };
    private static final String WHERE = MediaStore.Audio.Media.IS_MUSIC + " <> " + 0;
    private static final String SORT_ORDER = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE";

    private static AsyncQueryHandler sQueryHandler;
    private MusicPickerAdapter mAdapter;
    private ListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.music_picker);

        mAdapter = new MusicPickerAdapter(this, null);
        mListView = (ListView)findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setEmptyView(findViewById(R.id.empty_view));

        Button cancelBtn = (Button)findViewById(R.id.btn_cancel);
        cancelBtn.setOnClickListener(this);

        Button addBtn = (Button)findViewById(R.id.btn_add);
        addBtn.setOnClickListener(this);

        createActionBar();
        createAsyncQueryHandler(getContentResolver(), mAdapter);
        sQueryHandler.startQuery(
                0,
                null,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                PROJECTION,
                WHERE,
                null,
                SORT_ORDER);
    }

    @Override
    public void onStart() {
        super.onStart();
        TrackingUtil.startSession(getApplicationContext(), this);
        TrackingUtil.setScreen(getApplicationContext(), TrackingUtil.SCREEN_MUSIC_PICKER);
        ConnectionManager.setConnectionManagerListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        TrackingUtil.stopSession(getApplicationContext(), this);
    }

    @Override
    public void onDestroy() {
        ConnectionManager.removeConnectionManagerListener(this);
        mAdapter.changeCursor(null);
        mListView.setAdapter(null);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
        case R.id.btn_cancel:
            setResult(RESULT_CANCELED, null);
            finish();
            break;
        case R.id.btn_add:
            ArrayList<Parcelable> uriList = mAdapter.getMarkedUriList();
            Intent result = new Intent();
            result.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
            setResult(RESULT_OK, result);
            finish();
            break;
        default:
            break;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED, null);
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            setResult(RESULT_CANCELED, null);
            finish();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void disconnected(String reason) {
        super.disconnected(reason);
        LogUtil.v(LogUtil.LOG_TAG, "MusicPickerActivity.disconnected");

        if (!mForeground) {
            Intent intent = new Intent();
            intent.setClass(this, StartupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        finish();
    }

    private void createActionBar() {
        final ActionBar bar = getActionBar();
        bar.setTitle(R.string.party_share_strings_music_list_add_song_title_txt);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE,
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        bar.setDisplayHomeAsUpEnabled(true);
    }

    private static void createAsyncQueryHandler(ContentResolver cr, MusicPickerAdapter adapter) {
        sQueryHandler = new AddSongsAsyncQueryHandler(cr, adapter);
    }

    private static class AddSongsAsyncQueryHandler extends AsyncQueryHandler {
        MusicPickerAdapter mHandlerAdapter;

        public AddSongsAsyncQueryHandler(ContentResolver cr, MusicPickerAdapter adapter) {
            super(cr);
            mHandlerAdapter = adapter;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            super.onQueryComplete(token, cookie, cursor);
            mHandlerAdapter.changeCursor(cursor);
        }
    }
}
