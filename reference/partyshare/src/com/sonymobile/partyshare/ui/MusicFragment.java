/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.httpd.MusicPlayListController;
import com.sonymobile.partyshare.httpd.PartyShareCommand;
import com.sonymobile.partyshare.httpd.PostMusicContent;
import com.sonymobile.partyshare.provider.MusicProvider;
import com.sonymobile.partyshare.provider.MusicProviderUtil;
import com.sonymobile.partyshare.provider.ProviderUtil;
import com.sonymobile.partyshare.service.MusicService;
import com.sonymobile.partyshare.service.MusicService.State;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.DeviceInfo;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.Music;
import com.sonymobile.partyshare.util.Utility;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The list of shared music in Party. The music is played only on a host device.
 */
public class MusicFragment extends BaseFragment implements
        OnClickListener, LoaderCallbacks<Cursor> {
    private PartyShareActivity mActivity;
    private LinearLayout mAddBtn;
    private ImageButton mSkipButton;
    private ImageButton mRewindButton;
    private PlayPauseButton mPlayPauseButton;

    private static SimpleCursorAdapter mMusicListAdapter;
    private View mView;
    private ListView mListView;

    // Playview area
    private View mPlaybackView;
    private View mHostInfoView;
    private TextView mMusicTitle;
    private TextView mMusicArtist;
    private TextView mHostInfo;
    private ImageView mMusicAlbumArt;

    private BtnState mBtnState;

    //Service
    private MusicService mBoundService = null;
    private Music mAddSongAfterBind = null;

    private static final int MSG_INIT_PLAYVIEW = 100;
    private static final int MSG_SET_PLAYVIEW = 101;
    private static final int MSG_DISMISS_POPUP = 102;
    private static final int MSG_CHANGE_TO_PLAY = 104;
    private static final int MSG_CHANGE_TO_PAUSE = 105;

    private static final String KEY_PLAYVIEW_TITLE = "play_title";
    private static final String KEY_PLAYVIEW_ARTIST = "play_artist";

    private static int mCurrentId = -1;
    private boolean mMoveToCurrent = true;
    private PlayViewHandler mVisibleHandler;

    private Handler mHandler = new Handler();

    enum BtnState {
        Play,
        Pause
    };

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    public MusicFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        LogUtil.v(LogUtil.LOG_TAG, "MusicFragment.onCreateView");
        mView = getActivity().getLayoutInflater().inflate(R.layout.music_fragment, null);

        mPlaybackView = mView.findViewById(R.id.music_info);
        mHostInfoView = mView.findViewById(R.id.host_info);
        mHostInfo = (TextView) mHostInfoView.findViewById(R.id.text_music_playview_host_info);

        // Set play view for Host
        if (ConnectionManager.isGroupOwner()) {
            mPlaybackView.setVisibility(View.VISIBLE);
            setControlButton();
            mMusicTitle = (TextView)mPlaybackView.findViewById(
                    R.id.text_music_playview_title_owner);
            mMusicArtist = (TextView)mPlaybackView.findViewById(
                    R.id.text_music_playview_artist_name_owner);
            mMusicAlbumArt = (ImageView)mPlaybackView.findViewById(
                    R.id.image_playview_album_art_owner);
            changeButtonToPlay();
        } else {
            mHostInfoView.setVisibility(View.VISIBLE);
            mMusicTitle = (TextView) mHostInfoView.findViewById(
                    R.id.text_music_playview_title_guest);
            mMusicArtist = (TextView) mHostInfoView.findViewById(
                    R.id.text_music_playview_artist_name_guest);
            mMusicAlbumArt = (ImageView) mHostInfoView.findViewById(
                    R.id.image_playview_album_art_guest);
        }
        initPlayView();

        // Add button
        mAddBtn = (LinearLayout)mView.findViewById(R.id.add_btn);
        mAddBtn.setOnClickListener(this);

        // Set adapter
        final String[] from = {
                MusicProvider.COLUMN_TITLE,
                MusicProvider.COLUMN_ARTIST,
                MusicProvider.COLUMN_MUSIC_URI
        };

        final int[] to = {
                R.id.text_playlist_music_title, R.id.text_playlist_artist_name,
                R.id.text_playlist_owner_name,
        };

        setMusicListAdapter(getActivity(), from, to);

        mListView = (ListView)mView.findViewById(R.id.music_list_view);
        mListView.setEmptyView(mView.findViewById(R.id.empty_view));
        mListView.setAdapter(mMusicListAdapter);

        // Set ItemClickListener (Host only)
        if (ConnectionManager.isGroupOwner()) {
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    // set music_id
                    MusicListAdapter.ViewHolder holder =
                            (MusicListAdapter.ViewHolder) view.getTag();
                    Intent intent = new Intent();
                    int musicId = holder.playlistId;

                    intent.putExtra("LIST_ID", musicId);
                    intent.setAction(MusicService.ACTION_ITEM_CLICK);
                    getActivity().startService(intent);
                }
            });
        }

        // Set handler
        mVisibleHandler = new PlayViewHandler(this);
        return mView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (PartyShareActivity)activity;
    }

    @Override
    public void onResume() {
        super.onResume();
        doBindService();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!ConnectionManager.isGroupOwner()) {
            ProviderUtil.delete(getActivity(), MusicProvider.CONTENT_URI, null, null);
        }
        doUnbindService();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case CHOOSE_FILE_RESULT_CODE:
                chooseFileResult(data);
                break;
            default :
                break;
        }
    }

    private static void setMusicListAdapter(Context context, String[] from, int[] to) {
        mMusicListAdapter =
                new MusicListAdapter(context, R.layout.music_list, null, from, to, 0);
    }

    private void chooseFileResult(final Intent data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Uri> uriList = data.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (uriList == null) {
                    Uri uri = data.getData();
                    if (uri == null) {
                        return;
                    } else {
                        uriList = new ArrayList<Uri>();
                        uriList.add(uri);
                    }
                }

                for (Uri uri : uriList) {
                    Cursor c = null;
                    try {
                        c = ProviderUtil.query(mActivity, uri, null, null, null, null);
                        registAddSong(c);
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }
                }
            }
        }).start();
    }

    private void registAddSong(Cursor c) {
        if (c == null) {
            return;
        }

        c.moveToFirst();
        String title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
        String artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
        String time = c.getString(c.getColumnIndex(MediaStore.Audio.Media.DURATION));
        String filePath = c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA));
        String mimetype = c.getString(c.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));

        // insert local music path.
        ContentValues values = new ContentValues();
        values.put(MusicProvider.COLUMN_MUSIC_LOCAL_PATH, filePath);
        values.put(MusicProvider.COLUMN_MUSIC_MIMETYPE, mimetype);
        Uri localMusic = ProviderUtil.insert(
                getActivity(), MusicProvider.CONTENT_URI_LOCAL_MUSIC, values);

        String songUrl = null;
        if (localMusic != null) {
            songUrl = String.format("/%s/%s", "music", localMusic.getLastPathSegment());
            LogUtil.d(LogUtil.LOG_TAG, "MusicFragment.registAddSong songUrl : " + songUrl);
        }

        // Get mac add from Connection manager
        ConnectionManager manager =
                ConnectionManager.getInstance(mActivity.getApplicationContext());
        String macAddress = ConnectionManager.getMyAddress();

        String ownerName = "";
        for (DeviceInfo info : manager.getGroupList()) {
            if (info.getDeviceAddress().equals(macAddress)) {
                ownerName = info.getUserName();
                break;
            }
        }

        int duration = 0;
        if (time != null && !time.isEmpty()) {
            duration = Integer.parseInt(time);
        }

        HashMap<String, String> map = new HashMap<String, String>();
        if (ConnectionManager.isGroupOwner()) {
            if (MusicProviderUtil.isPlayListMaximum(mActivity)) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Utility.showToast(mActivity,
                                getResources().getString(
                                R.string.party_share_strings_music_list_full_txt),
                                Toast.LENGTH_SHORT);
                    }
                });
            } else {
                Music music = new Music(title, artist, macAddress, ownerName, duration, songUrl);
                addToPlaylist(music);
            }
        } else {
            map.put(PartyShareCommand.PARAM_MUSIC_TITLE, title);
            map.put(PartyShareCommand.PARAM_MUSIC_ARTIST, artist);
            map.put(PartyShareCommand.PARAM_MUSIC_TIME, time);
            map.put(PartyShareCommand.PARAM_MUSIC_URL, songUrl);
            map.put(PartyShareCommand.PARAM_MUSIC_OWNER_ADDRESS, macAddress);
            map.put(PartyShareCommand.PARAM_MUSIC_OWNER_NAME, ownerName);
            new PostMusicContent(mActivity).post(map);
        }
    }

    private void setControlButton() {
        mBtnState = BtnState.Play;

        mSkipButton = (ImageButton)mPlaybackView.findViewById(R.id.skipbutton);
        mRewindButton = (ImageButton)mPlaybackView.findViewById(R.id.rewindbutton);
        mPlayPauseButton = (PlayPauseButton)mPlaybackView.findViewById(R.id.play_pause_button);

        mSkipButton.setOnClickListener(this);
        mRewindButton.setOnClickListener(this);
        mPlayPauseButton.setOnClickListener(this);
    }

    private void initPlayView() {
        LogUtil.d(LogUtil.LOG_TAG, "MusicFragment initPlayView");
        mMusicTitle.setText(getString(R.string.party_share_strings_music_list_no_song_txt));
        mMusicTitle.setTextColor(getActivity().getResources().getColor(R.color.sub_text_color));
        mMusicArtist.setText("");
        mMusicAlbumArt.setImageDrawable(null);
        mMusicAlbumArt.setVisibility(View.GONE);
        setCurrentId(-1);

        if (ConnectionManager.isGroupOwner()) {
            changeButtonToPlay();
            setControlButtonEnable(false);
        } else {
            if (mHostInfo != null) {
                mHostInfo.setText("");
            }
        }
    }

    private boolean isControlButtonEnable() {
        if (mSkipButton.isEnabled() && mRewindButton.isEnabled()
                && mPlayPauseButton.isEnabled()) {
            return true;
        }
        return false;
    }

    private void setControlButtonEnable(boolean state) {
        mSkipButton.setEnabled(state);
        mRewindButton.setEnabled(state);
        mPlayPauseButton.setEnabled(state);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();

        if (v == mAddBtn) {
            intent.setClass(mActivity, MusicPickerActivity.class);
            startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
        }

        if (ConnectionManager.isGroupOwner() && MusicProviderUtil.getListSize(mActivity) > 0) {

            if (v == mPlayPauseButton && mBtnState == BtnState.Play) {
                changeButtonToPause();
                intent.setAction(MusicService.ACTION_PLAY);
                getActivity().startService(intent);
            }
            else if (v == mSkipButton) {
                intent.setAction(MusicService.ACTION_SKIP);
                getActivity().startService(intent);
            }
            else if (v == mRewindButton) {
                intent.setAction(MusicService.ACTION_REWIND);
                getActivity().startService(intent);
            }
            else if (v == mPlayPauseButton && mBtnState == BtnState.Pause) {
                changeButtonToPlay();
                intent.setAction(MusicService.ACTION_PAUSE);
                getActivity().startService(intent);
            }
        }
    }

    private void doInitLoader() {
        if (!ConnectionManager.isGroupOwner()) {
            MusicPlayListController.reloadPlayList(mActivity);
        }
        getActivity().getLoaderManager().initLoader(0, null, (LoaderCallbacks<Cursor>)this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle arg) {

        return new CursorLoader(getActivity(), MusicProvider.CONTENT_URI, null,
                null, null, MusicProvider.COLUMN_PLAY_NUMBER + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        mMusicListAdapter.swapCursor(c);

        // Always dismiss pop-up
        mVisibleHandler.sendEmptyMessage(MSG_DISMISS_POPUP);

        int loadStatus = MusicProvider.MUSIC_STATUS_IDLE;
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            do {
                loadStatus = c.getInt(c.getColumnIndex(MusicProvider.COLUMN_STATUS));
                int id = c.getInt(c.getColumnIndex(MusicProvider.COLUMN_MUSIC_ID));

                if (loadStatus == MusicProvider.MUSIC_STATUS_FOCUSED && mCurrentId != id) {
                    // Focus changed - update playview
                    LogUtil.d(LogUtil.LOG_TAG,
                            "MusicFragment onLoadFinished  set playview - ID : "
                            + c.getInt(c.getColumnIndex(MusicProvider.COLUMN_MUSIC_ID))
                            + ", mCurrentId : " + mCurrentId);
                    setCurrentId(id);
                    final int position =
                            c.getInt(c.getColumnIndex(MusicProvider.COLUMN_PLAY_NUMBER)) -1;

                    if (mMoveToCurrent) {
                        scrollPlaylist(position);
                    }

                    Message msg = mVisibleHandler.obtainMessage(MSG_SET_PLAYVIEW);
                    Bundle bundle = new Bundle();
                    bundle.putString(KEY_PLAYVIEW_TITLE,
                            c.getString(c.getColumnIndex(MusicProvider.COLUMN_TITLE)));
                    bundle.putString(KEY_PLAYVIEW_ARTIST,
                            c.getString(c.getColumnIndex(MusicProvider.COLUMN_ARTIST)));
                    msg.obj = bundle;
                    msg.arg1 = position;
                    mVisibleHandler.sendMessage(msg);
                    return;
                }
            } while (c.moveToNext());
            return;
        } else {
            setCurrentId(-1);
            mVisibleHandler.sendEmptyMessage(MSG_INIT_PLAYVIEW);
            LogUtil.i(LogUtil.LOG_TAG, "cursor NO ITEM !!! - init playview");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mMusicListAdapter.swapCursor(null);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // getService
            mBoundService = ((MusicService.MusicServiceBinder)service).getService();
            mBoundService.setMusicEventListener(mMusicEventListener);
            if (mAddSongAfterBind != null) {
                mBoundService.addMusicRequest(mAddSongAfterBind);
                mAddSongAfterBind = null;
            }
            doInitLoader();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
        }
    };

    void doBindService() {
        //Bind MusicService
        getActivity().bindService(new Intent(getActivity().getApplicationContext(),
                MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        if (mBoundService != null) {
            mBoundService.setMusicEventListener(null);
            getActivity().unbindService(mConnection);
        }
    }

    private void addToPlaylist(Music music) {
        if (mBoundService != null) {
            mBoundService.addMusicRequest(music);
        } else {
            mAddSongAfterBind = music;
        }
    }

    public void changeButtonToPlay() {
        mBtnState = BtnState.Play;
        mPlayPauseButton.setPlaying(false, true);
    }
    public void changeButtonToPause() {
        mBtnState = BtnState.Pause;
        mPlayPauseButton.setPlaying(true, true);
    }

    public void setShowFlag() {
        mMoveToCurrent = false;
    }

    public void scrollPlaylist(int reqPosition) {
        final int position;
        if(reqPosition < 0) {
            // Scroll to current track
            reqPosition = 0;
            Music currentMusic = MusicProviderUtil.getListItemById(mActivity, mCurrentId);
            if (currentMusic != null) {
                if (currentMusic.getOrder() > 0) {
                    reqPosition = currentMusic.getOrder() - 1;
                }
            }
        }
        position = reqPosition;
        mListView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListView.setSelection(position);
            }
        }, 100);
    }

    public static int getCurrentId() {
        return mCurrentId;
    }

    public static void setCurrentId(int id) {
        mCurrentId = id;
    }

    static class PlayViewHandler extends Handler {
        private final WeakReference<MusicFragment> refFragment;

        public PlayViewHandler(MusicFragment musicFragment) {
            refFragment = new WeakReference<MusicFragment>(musicFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            MusicFragment musicFrag = refFragment.get();
            if (musicFrag == null) {
                return;
            }
            switch (msg.what) {
            case MSG_INIT_PLAYVIEW:
                LogUtil.d(LogUtil.LOG_TAG, "handleMessage : MSG_INIT_PLAYVIEW");
                musicFrag.initPlayView();
                break;

            case MSG_SET_PLAYVIEW:
                LogUtil.d(LogUtil.LOG_TAG, "handleMessage : MSG_SET_PLAYVIEW");
                String title = ((Bundle)msg.obj).getString(KEY_PLAYVIEW_TITLE);
                musicFrag.mMusicTitle.setText(title);
                musicFrag.mMusicTitle.setTextColor(
                        musicFrag.getActivity().getResources().getColor(R.color.main_text_color));
                musicFrag.mMusicTitle.setSelected(true);
                musicFrag.mMusicArtist.setText(((Bundle)msg.obj).getString(KEY_PLAYVIEW_ARTIST));

                // Set album art
                int resId = Utility.getAlbumArtResId(musicFrag.getActivity(), title);
                if (musicFrag.mMusicAlbumArt.getVisibility() != View.VISIBLE) {
                    musicFrag.mMusicAlbumArt.setVisibility(View.VISIBLE);
                }
                musicFrag.mMusicAlbumArt.setImageDrawable(
                        musicFrag.getResources().getDrawable(resId));

                if (!ConnectionManager.isGroupOwner()) {
                    //Set host info.
                    musicFrag.mHostInfo.setText(musicFrag.getResources().getString(
                            R.string.party_share_strings_music_list_playing_on_host_txt));

                    //Set click listener to host info view.
                    musicFrag.mHostInfoView.setOnClickListener(musicFrag.mPlayViewListener);
                    break;
                }

                // switch toggle button
                if (!musicFrag.isControlButtonEnable()) {
                    musicFrag.setControlButtonEnable(true);
                }
                State playerState = musicFrag.mBoundService.getPlayerState();
                if (playerState == State.Playing || playerState == State.Preparing) {
                    musicFrag.changeButtonToPause();
                } else {
                    musicFrag.changeButtonToPlay();
                }
                musicFrag.mPlaybackView.setOnClickListener(musicFrag.mPlayViewListener);

                break;

            case MSG_DISMISS_POPUP:
                // Close option menu
                PopupMenu popup = (PopupMenu) ((MusicListAdapter)
                        MusicFragment.mMusicListAdapter).getPopup();
                if (popup != null) {
                    popup.dismiss();
                }
                break;
            case MSG_CHANGE_TO_PLAY:
                musicFrag.changeButtonToPlay();
                break;
            case MSG_CHANGE_TO_PAUSE:
                musicFrag.changeButtonToPause();
                break;
            default:
                break;
            }
        }
    }

    private OnClickListener mPlayViewListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Cursor c = null;
            try {
                c = ProviderUtil.query(
                        getActivity(), MusicProvider.CONTENT_URI,
                        new String[] {MusicProvider.COLUMN_PLAY_NUMBER},
                        MusicProvider.COLUMN_MUSIC_ID + "=" + mCurrentId, null, null);
                if (c != null && c.moveToFirst()) {
                    final int position = c.getInt(0) -1;
                    mListView.post(new Runnable() {

                        @Override
                        public void run() {
                            LogUtil.d(LogUtil.LOG_TAG,
                                    "mPlayViewListener.OnClick() - mCurrentId : " + mCurrentId);
                            mListView.setSelection(position);
                        }
                    });
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
    };

    private final MusicEventListener mMusicEventListener = new MusicEventListener() {
        @Override
        public void musicStopped() {
            setCurrentId(-1);
            if (ConnectionManager.isGroupOwner()) {
                mVisibleHandler.sendEmptyMessage(MSG_INIT_PLAYVIEW);
            }
        }

        @Override
        public void onMusicPause() {
            if (ConnectionManager.isGroupOwner()) {
                mVisibleHandler.sendEmptyMessage(MSG_CHANGE_TO_PLAY);
            }
        }

        @Override
        public void onMusicStart() {
            if (ConnectionManager.isGroupOwner()) {
                mVisibleHandler.sendEmptyMessage(MSG_CHANGE_TO_PAUSE);
            }
        }
    };
}
