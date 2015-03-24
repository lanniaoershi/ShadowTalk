/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2014 Sony Mobile Communications Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the Sony Mobile Communications Inc
 * End User License Agreement ("EULA"). Any use of the modifications is subject
 * to the terms of the EULA.
 */
package com.sonymobile.partyshare.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.httpd.MusicJsonFile;
import com.sonymobile.partyshare.provider.MusicProvider;
import com.sonymobile.partyshare.provider.MusicProviderUtil;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.ui.MusicEventListener;
import com.sonymobile.partyshare.util.AudioFocusHelper;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.Music;
import com.sonymobile.partyshare.util.MusicFocusable;
import com.sonymobile.partyshare.util.Utility;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Service that handles media playback. This is the Service through which we perform all the media
 * handling in our application. Upon initialization, it starts a {@link MusicProviderUtil} to scan
 * the PartyShare db. Then, it waits for Intents (which come from our main activity,
 * {@link MainActivity}, which signal the service to perform specific operations: Play, Pause,
 * Rewind, Skip, etc.
 */
public class MusicService extends Service implements OnCompletionListener, OnPreparedListener,
        OnErrorListener, MusicFocusable {

    public static final String ACTION_TOGGLE_PLAYBACK =
            "com.sonymobile.partyshare.action.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "com.sonymobile.partyshare.action.PLAY";
    public static final String ACTION_PAUSE = "com.sonymobile.partyshare.action.PAUSE";
    public static final String ACTION_STOP = "com.sonymobile.partyshare.action.STOP";
    public static final String ACTION_SKIP = "com.sonymobile.partyshare.action.SKIP";
    public static final String ACTION_REWIND = "com.sonymobile.partyshare.action.REWIND";
    public static final String ACTION_URL = "com.sonymobile.partyshare.action.URL";
    public static final String ACTION_ITEM_CLICK =
            "com.sonymobile.partyshare.action.ITEM_CLICK";
    public static final String ACTION_REMOVE = "com.sonymobile.partyshare.action.REMOVE";
    public static final String ACTION_PLAY_NEXT =
            "com.sonymobile.partyshare.action.PLAY_NEXT";
    public static final String ACTION_DELETE_MEMBER =
            "com.sonymobile.partyshare.action.DELETE_MEMBER";
    public static final String ACTION_ADD = "com.sonymobile.partyshare.action.ADD";
    public static final float DUCK_VOLUME = 0.3f;

    private MediaPlayer mPlayer = null;

    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    private AudioFocusHelper mAudioFocusHelper = null;

    // indicates the state our service:
    public enum State {
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
                    // paused in this state if we don't have audio focus. But we stay in this state
                    // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    };

    private State mState = State.Stopped;

    private enum PauseReason {
        UserRequest,  // paused by user request
        Ringing,
    };

    // why did we pause? (only relevant if mState == State.Paused)
    private PauseReason mPauseReason = PauseReason.UserRequest;

    // do we have audio focus?
    private enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }
    private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
    private AudioManager mAudioManager;
    private TelephonyManager mTelephonyManager;
    private MusicServiceHandler mServiceHandler;
    private long mDelayTime = 500;
    private int mMusicVolume = 0;
    private boolean mRepeat = true;
    private static final int MSG_SHOW_ERROR_TOAST = 201;
    private static final int MSG_RESET_VOLUME = 202;
    private MusicEventListener mMusicEventListener;

    public void setMusicEventListener(MusicEventListener musicEventListener) {
        mMusicEventListener = musicEventListener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.onCreate");

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        mMusicVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        mServiceHandler = new MusicServiceHandler(this);

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        initializeAudioFocus();
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.onStartCommand");
        if (intent != null) {
            String action = intent.getAction();
            if (action == null) {
                action = "";
            }
            LogUtil.d(LogUtil.LOG_TAG, "intent action : " + action);
            if (action.equals(ACTION_TOGGLE_PLAYBACK)) {
                processTogglePlaybackRequest();
            } else if (action.equals(ACTION_PLAY)) {
                processPlayRequest();
            } else if (action.equals(ACTION_PAUSE)) {
                mPauseReason = PauseReason.UserRequest;
                processPauseRequest();
            } else if (action.equals(ACTION_SKIP)) {
                processSkipRequest(true);
            } else if (action.equals(ACTION_STOP)) {
                processStopRequest();
            } else if (action.equals(ACTION_REWIND)) {
                processRewindRequest();
            } else if (action.equals(ACTION_ITEM_CLICK)) {
                int listId = intent.getIntExtra("LIST_ID", 0);
                processChangeRequest(listId);
            } else if (action.equals(ACTION_REMOVE)) {
                int deleteId = intent.getExtras().getInt("delete_id");
                processRemoveRequest(deleteId);
            } else if (action.equals(ACTION_PLAY_NEXT)) {
                int nextId = intent.getExtras().getInt("play_next");
                processPlayNextRequest(nextId);
            } else if (action.equals(ACTION_DELETE_MEMBER)) {
                String member = intent.getStringExtra("delete_member");
                processDeleteMember(member);
            } else if (action.equals(ACTION_ADD)) {
                Bundle args = intent.getExtras();
                String title = args.getString(MusicProvider.COLUMN_TITLE, "");
                String artist = args.getString(MusicProvider.COLUMN_ARTIST, "");
                String owner = args.getString(MusicProvider.COLUMN_OWNER_ADDRESS, "");
                String name = args.getString(MusicProvider.COLUMN_OWNER_NAME, "");
                int time = args.getInt(MusicProvider.COLUMN_TIME, 0);
                String uri = args.getString(MusicProvider.COLUMN_MUSIC_URI, "");
                Music addMusic = new Music(title, artist, owner, name, time, uri);
                addMusicRequest(addMusic);
            }
        }
        return START_NOT_STICKY; // Means we started the service, but don't want it to
                                 // restart in case it's killed.
    }

    private void processDeleteMember(String member) {
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.processDeleteMember member : " + member);
        State lastState = getPlayerState();
        int currentId = MusicProviderUtil.getCurrentTrack(this);
        Music music = MusicProviderUtil.getListItemById(this, currentId);
        int listsize = MusicProviderUtil.getListSize(this);

        // Stop if member's music is playing
        if (currentId > 0 && music != null && member.equals(music.getOwner())) {
            // Stop the music, temporary
            processStopRequest();
        }

        if (listsize > 0 && (lastState == State.Playing || lastState == State.Paused)
                && getPlayerState() == State.Stopped) {

              mRepeat = true;
              tryToGetAudioFocus();
              int nextId = MusicProviderUtil.getNextTrack(this, currentId, true, member);
              if (lastState == State.Playing) {
                  LogUtil.d(LogUtil.LOG_TAG, "processDeleteMember() - play next");
                  playNextSong(nextId);
              } else if (lastState == State.Paused) {
                  LogUtil.d(LogUtil.LOG_TAG, "processDeleteMember() - move focus to next");
                  MusicProviderUtil.setPlayingStatus(this, nextId, true);
              }
        }
    }

    private void processPlayNextRequest(int reqId) {
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.processPlayNextRequest reqId : " + reqId);
        // Set Id to param.
        MusicProviderUtil.playNext(this, reqId);

        MusicJsonFile.refreshJsonFile(getApplicationContext());
    }

    private void processRemoveRequest(int deleteId) {
        int playId = MusicProviderUtil.getCurrentTrack(this);
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.processRemoveRequest deleteId : " + deleteId +
                ", playId : " + playId);
        if (playId == deleteId) {
            mRepeat = true;
            // Handle mediaplayer when List has more than one item
            if (MusicProviderUtil.getListSize(this) > 1) {
                // move to next track
                processSkipRequest(false);
            } else {
                LogUtil.d(LogUtil.LOG_TAG, "playlist is empty");
                processStopRequest();
            }
        }
        MusicProviderUtil.removeMusic(this, deleteId);

        MusicJsonFile.refreshJsonFile(getApplicationContext());
    }

    private void processTogglePlaybackRequest() {
        if (getPlayerState() == State.Paused || getPlayerState() == State.Stopped) {
            processPlayRequest();
        } else {
            processPauseRequest();
        }
    }

    private void processPlayRequest() {
        State state = getPlayerState();
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.processPlayRequest - state : " + state);
        tryToGetAudioFocus();

        // actually play the song

        if (state == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong(0);
        }
        else if (mPlayer != null && state == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            setPlayerState(State.Playing);
            configAndStartMediaPlayer();
        }
    }

    private void processPauseRequest() {
        if (getPlayerState() == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            setPlayerState(State.Paused);
            if (mPlayer != null) {
                mPlayer.pause();
            }
            if (mMusicEventListener != null) {
                mMusicEventListener.onMusicPause();
            }
            relaxResources(false); // while paused, we always retain the MediaPlayer
            // do not give up audio focus
        }
    }

    private void processRewindRequest() {
        int playingPosition = 0;
        if (mPlayer != null) {
            playingPosition = mPlayer.getCurrentPosition();
        }
        mRepeat = true;
        State state = getPlayerState();
        if (playingPosition < 5000) {
            // move to prev. track
            tryToGetAudioFocus();
            int prevId = 0;
            prevId = MusicProviderUtil.getNextTrack(
                    this, MusicProviderUtil.getCurrentTrack(this), false);
            if (state == State.Paused || state == State.Stopped) {
                // stop and set focus
                processStopRequest();
                MusicProviderUtil.setPlayingStatus(this, prevId, true);

                MusicJsonFile.refreshJsonFile(getApplicationContext());
            } else {
                playNextSong(prevId);
            }
        } else {
            if (mPlayer != null) {
                // Replay from the beginning
                mPlayer.seekTo(0);
            }
        }
    }

    private void processSkipRequest(boolean isManual) {
        if (isManual) {
            mRepeat = true;
        }
        tryToGetAudioFocus();
        int nextId = 0;
        nextId = MusicProviderUtil.getNextTrack(
                this, MusicProviderUtil.getCurrentTrack(this), true);
        State state = getPlayerState();
        if (state == State.Paused || state == State.Stopped) {
            // stop and set focus
            processStopRequest();
            MusicProviderUtil.setPlayingStatus(this, nextId, true);

            MusicJsonFile.refreshJsonFile(getApplicationContext());
        } else {
            playNextSong(nextId);
        }
    }

    private void processStopRequest() {
        processStopRequest(false);
    }

    private void processStopRequest(boolean force) {
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.processStopRequest - force : " + force);
        State state = getPlayerState();
        if (state == State.Playing || state == State.Paused || force) {
            setPlayerState(State.Stopped);

            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();

            // service is no longer necessary. Will be started again if needed.
            stopSelf();
        }
    }

    private void processChangeRequest(int reqId) {
        if (MusicProviderUtil.getCurrentTrack(this) == reqId) {
            State state = getPlayerState();
            // Handle request for same track
            if (mPlayer != null && state == State.Paused) {
                mPlayer.start();
                setPlayerState(State.Playing);
                if (mMusicEventListener != null) {
                    mMusicEventListener.onMusicStart();
                }
            } else if (state == State.Stopped) {
                mRepeat = true;
                playNextSong(reqId);
            }
        } else {
            if (mPlayer != null) {
                mPlayer.stop();
            }
            mRepeat = true;
            tryToGetAudioFocus();
            playNextSong(reqId);
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    private void configAndStartMediaPlayer() {
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.configAndStartMediaPlayer - mAudioFocus : " +
                mAudioFocus);
        if (mPlayer == null) {
            return;
        }
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause.
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                if (mMusicEventListener != null) {
                    mMusicEventListener.onMusicPause();
                }
                return;
            }
        } else if (mAudioFocus == AudioFocus.NoFocusCanDuck) {
            mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
        } else {
            mPlayer.setVolume(1.0f, 1.0f); // we can be loud
        }

        if (!mPlayer.isPlaying()) {
            setPlayerState(State.Playing);
            mPlayer.start();
            if (mMusicEventListener != null) {
                mMusicEventListener.onMusicStart();
            }
        }
    }

    private void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                        && mAudioFocusHelper.requestFocus()) {
            mAudioFocus = AudioFocus.Focused;
        }
    }

    /**
     * Starts playing the next song.
     * If reqId is not specified, the next song will be selected.
     * If reqId is specified, then it hand-in the URL or
     * path to the song that will be played next.
     */
    private void playNextSong(int reqId) {
        State state = getPlayerState();
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.playNextSong - state : " + state +
                ", mRepeat : " + mRepeat);

        relaxResources(state == State.Preparing ? true : false);
        setPlayerState(State.Stopped);

        try {
            Music playingItem = null;

            if (reqId <= 0) {
                // find focused track
                reqId = MusicProviderUtil.getCurrentTrack(this);
                LogUtil.d(LogUtil.LOG_TAG, "getCurrentTrack() : " + reqId);
            }
            playingItem = MusicProviderUtil.getListItemById(this, reqId);
            if (playingItem == null) {
                processStopRequest(true); // stop everything!
                return;
            }
            if (playingItem.getOrder() == 1) {
                if (!mRepeat) {
                    LogUtil.w(LogUtil.LOG_TAG,
                            "return to first : EMPTY PLAYLIST!!! stop the loop.");

                    // Show error toast.
                    Message msg = new Message();
                    msg.what = MSG_SHOW_ERROR_TOAST;
                    mServiceHandler.sendMessage(msg);

                    MusicProviderUtil.setPlayingStatus(this, reqId, true);
                    processStopRequest(true);
                    if (mMusicEventListener != null) {
                        mMusicEventListener.onMusicPause();
                    }
                    mRepeat = true;
                    MusicJsonFile.refreshJsonFile(getApplicationContext());
                    return;
                }
                LogUtil.d(LogUtil.LOG_TAG, "return to first : reset repeat flag");
                mRepeat = false;
            }

            // set the source of the media player in content URI
            createMediaPlayerIfNeeded();
            LogUtil.d(LogUtil.LOG_TAG, "playingItem.getURI() :" + playingItem.getURI() +
                    ":" + playingItem.getMusicTitle());
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            Uri uri = MusicProviderUtil.getMusicUri(this, playingItem);
            mPlayer.setAudioSessionId(reqId);
            mPlayer.setDataSource(getApplicationContext(), uri);

            MusicProviderUtil.setPlayingStatus(this, reqId, true);

            setPlayerState(State.Preparing);

            // starts preparing the media player in the background. When it's done, it will call
            // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
            // the listener to 'this').
            //
            // Until the media player is prepared, we *cannot* call start() on it!
            mPlayer.prepareAsync();

            MusicJsonFile.refreshJsonFile(getApplicationContext());
        }
        catch (IOException ex) {
            LogUtil.e(LogUtil.LOG_TAG, "IOException playing next song : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /** Called when media player is done playing current song. */
    public void onCompletion(MediaPlayer player) {
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.onCompletion - music finished!");

        // Stop music when list has no item
        if (MusicProviderUtil.getListSize(this) == 0) {
            processStopRequest();
            return;
        }
        if (!mRepeat) {
            mRepeat = true;
        }
        int next = MusicProviderUtil.getNextTrack(
                this, MusicProviderUtil.getCurrentTrack(this), true);
        playNextSong(next);
    }

    /** Called when media player is done preparing. */
    public void onPrepared(MediaPlayer player) {
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.onPrepared - music ready");

        Music playingItem = MusicProviderUtil.getListItemById(this, player.getAudioSessionId());
        if (playingItem == null) {
            processStopRequest(true);
            return;
        }
        // The media player is done preparing. That means we can start playing!
        setPlayerState(State.Playing);
        configAndStartMediaPlayer();
    }

    /**
     * Called when there's an error playing media. When this happens, the media player goes to
     * the Error state. We warn the user about the error and handle the media player.
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        LogUtil.e(LogUtil.LOG_TAG, "MusicService.onError: what=" + String.valueOf(what) +
                ", extra=" + String.valueOf(extra));

        // Stop music when list has no other item
        if (MusicProviderUtil.getListSize(this) < 1) {
            LogUtil.w(LogUtil.LOG_TAG, "MusicService.onError - no available item !!!");
            processStopRequest(true);
            if (mMusicEventListener != null) {
                mMusicEventListener.onMusicPause();
            }
            mRepeat = true;
            return true;
        }
        // get next track
        processSkipRequest(false);
        return true; // true indicates we handled the error
    }

    public void onGainedAudioFocus() {
        State state = getPlayerState();
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.onGainedAudioFocus - state : " + state);
        mAudioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (state == State.Playing) {
            configAndStartMediaPlayer();
        }
    }

    public void onLostAudioFocus(boolean canDuck) {
        State state = getPlayerState();
        LogUtil.d(LogUtil.LOG_TAG, "MusicService.onLostAudioFocus - state : " + state);
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (mPlayer.isPlaying()) {
            configAndStartMediaPlayer();
        }
    }

    // Add music to DB
    public void addMusicRequest(Music music) {
        String result = "";
        if (ConnectionManager.isGroupOwner() && MusicProviderUtil.getListSize(this) < 1) {
            result = MusicProviderUtil.addMusic(this, music);
            if (result.isEmpty()) {
                tryToGetAudioFocus();
                playNextSong(0);
                return;
            }
        } else {
            result = MusicProviderUtil.addMusic(this, music);
        }

        if (!result.isEmpty()) {
            LogUtil.e(LogUtil.LOG_TAG, "MusicService.addMusicRequest failed!!! - " + result);
        }

        MusicJsonFile.refreshJsonFile(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.onDestroy");
        super.onDestroy();
        // Service is being killed, so make sure we release our resources
        setPlayerState(State.Stopped);
        relaxResources(true);
        giveUpAudioFocus();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            phoneCallEvent(state, incomingNumber);
        }

        private void phoneCallEvent(int state, String incomingNumber) {
            State playerState = getPlayerState();
            switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                // if set ringtone volume 'OFF'
            if (playerState == State.Playing) {
                    mMusicVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    LogUtil.d(LogUtil.LOG_TAG,
                            "CALL_STATE_RINGING - mMusicVolume:" + mMusicVolume);
                }
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (playerState == State.Playing) {
                    mPlayer.setVolume(0.0f, 0.0f);
                    mPauseReason = PauseReason.Ringing;
                    processPauseRequest();
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                if (playerState == State.Paused && mPauseReason == PauseReason.Ringing) {
                    // Reset music volume
                    mServiceHandler.sendEmptyMessageDelayed(0, mDelayTime);
                }
                break;
            default:
                break;
            }
        }
    };

    public int getCurrentPosition() {
        if(getPlayerState() == State.Playing) {
            return mPlayer.getCurrentPosition();
        }
        else {
            return 0;
        }
    }

    //Binder
    public class MusicServiceBinder extends Binder {

        public MusicService getService() {
            return MusicService.this;
        }
    }

    // Binder create
    private final IBinder mBinder = new MusicServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        LogUtil.i(LogUtil.LOG_TAG, "MusicService.onRebind : " + intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtil.i(LogUtil.LOG_TAG, "MusicService.onUnbind : " + intent);
        return true;
    }

    public State getPlayerState() {
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.getPlayerState " + mState);
        return mState;
    }

    public void setPlayerState(State state) {
        LogUtil.v(LogUtil.LOG_TAG, "MusicService.setPlayerState " + state);
        mState = state;
    }

    private void initializeAudioFocus() {
        // create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)
        if (android.os.Build.VERSION.SDK_INT >= 8)
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        else
            mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus
    }

    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    private void createMediaPlayerIfNeeded() {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
        } else {
            mPlayer.reset();
        }
    }

    static class MusicServiceHandler extends Handler {
        private final WeakReference<MusicService> refMusicService;

        public MusicServiceHandler (MusicService musicService) {
            refMusicService = new WeakReference<MusicService>(musicService);
        }

        @Override
        public void handleMessage(Message msg) {
            MusicService service = refMusicService.get();
            if (service != null) {
                switch (msg.what) {
                case MSG_SHOW_ERROR_TOAST:
                    Utility.showToast(service.getApplicationContext(),
                            service.getResources().getString(
                                    R.string.party_share_strings_unable_to_play_err_txt),
                            Toast.LENGTH_SHORT);
                    LogUtil.d(LogUtil.LOG_TAG, "MusicServiceHandler - show error toast!!!");
                    break;
                case MSG_RESET_VOLUME:
                    LogUtil.d(LogUtil.LOG_TAG, "MusicServiceHandler - reset music volume : "
                            + service.mMusicVolume);
                    service.mAudioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC, service.mMusicVolume, 0);
                    service.processPlayRequest();
                    break;
                default:
                    break;
                }
            }
        }
    }
}
