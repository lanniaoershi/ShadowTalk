/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pConfig;
import android.os.Bundle;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.ga.TrackingUtil;
import com.sonymobile.partyshare.service.PhotoDownloadService;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.DeviceInfo;
import com.sonymobile.partyshare.session.WifiDirectStateMachine;
import com.sonymobile.partyshare.ui.MemberFragment.DeviceListActionListener;
import com.sonymobile.partyshare.ui.MemberFragment.IWifiDirectFragmentActionListener;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.PhotoUtils;
import com.sonymobile.partyshare.util.Setting;
import com.sonymobile.partyshare.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class PartyShareActivity extends BaseActivity implements DeviceListActionListener,
        IWifiDirectFragmentActionListener {
    final public static String EXTRA_SESSION_NAME = "extra_session";
    final public static String EXTRA_ALREADY_STARTED = "extra_started";

    final public static int TAB_POSITION_MEMBER = 0;
    final public static int TAB_POSITION_MUSIC = 1;
    final public static int TAB_POSITION_PHOTO = 2;

    private ActionBar mBar;
    private FragmentPagerAdapter mFragmentPagerAdapter;
    private List<BaseFragment> mFragments = new ArrayList<BaseFragment>();
    private MemberFragment mWifiDirectFragment;
    private PhotoFragment mPhotoFragment;
    private MusicFragment mMusicFragment;
    private ViewPager mViewPager;
    private Tab mTab;

    private boolean mStarted = false;
    private ProgressDialog mProgressDialog = null;
    private AlertDialog mErrorDialog = null;
    private AlertDialog mEndpartyDialog = null;

    private MenuItem mSlideshowItem;
    private MenuItem mDownloadItem;
    private MenuItem mSortPhotoItem;

    private List<DeviceInfo> mGroupList = new ArrayList<DeviceInfo>();

    @Override
    public List<DeviceInfo> getGroupList() {
        return mGroupList;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.party_share);

        Intent intent = getIntent();
        mUserName = Setting.getUserName(this);

        if (intent != null) {
            mStarted = intent.getBooleanExtra(EXTRA_ALREADY_STARTED, false);
            LogUtil.v(LogUtil.LOG_TAG, "mStarted:" + mStarted);
            mPartyName = intent.getStringExtra(EXTRA_SESSION_NAME);
        }

        mViewPager = (ViewPager)findViewById(R.id.view_pager);
        // WifiDirect
        mWifiDirectFragment = new MemberFragment();
        Bundle args1 = new Bundle();
        args1.putString(BaseFragment.KEY_TITLE, "Member");
        mWifiDirectFragment.setArguments(args1);

        // Music
        mMusicFragment = new MusicFragment();
        Bundle args2 = new Bundle();
        args2.putString(BaseFragment.KEY_TITLE, "Music");
        mMusicFragment.setArguments(args2);

        // Photo
        mPhotoFragment = new PhotoFragment();
        Bundle args3 = new Bundle();
        args3.putString(BaseFragment.KEY_TITLE, "Photos");
        mPhotoFragment.setArguments(args3);

        mFragments.add(mWifiDirectFragment);
        mFragments.add(mMusicFragment);
        mFragments.add(mPhotoFragment);
        mFragmentPagerAdapter = new BaseFragmentAdapter(getSupportFragmentManager(), mFragments);
        mViewPager.setAdapter(mFragmentPagerAdapter);

        if (savedInstanceState != null) {
            mWifiDirectFragment = (MemberFragment)mFragmentPagerAdapter.instantiateItem(
                    mViewPager, TAB_POSITION_MEMBER);
            mMusicFragment = (MusicFragment)mFragmentPagerAdapter.instantiateItem(
                    mViewPager, TAB_POSITION_MUSIC);
            mPhotoFragment = (PhotoFragment)mFragmentPagerAdapter.instantiateItem(
                    mViewPager, TAB_POSITION_PHOTO);
        }

        createActionBarCustomView();

        ConnectionManager.setConnectionManagerListener(this);
        initializeDisplay();
    }

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareActivity.onResume");
        super.onResume();

        if (Setting.getDownloadMode(this) == Setting.PHOTO_DOWNLOAD_MODE_AUTO) {
            if (PhotoUtils.isEnoughStorage(PhotoUtils.DOWNLOADABLE_CAPACITY_SIZE)) {
                LogUtil.v(LogUtil.LOG_TAG,
                        "PartyShareActivity.onResume Start the auto download of photo.");
                // Start the auto download of photo.
                Intent intent = new Intent(PhotoDownloadService.ACTION_START_AUTO_DL);
                startService(intent);
            }
        }
        updateDisplay();
    }

    @Override
    protected void onStart() {
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareActivity.onStart");
        super.onStart();
        TrackingUtil.startSession(getApplicationContext(), this);
        if (mViewPager.getCurrentItem() == TAB_POSITION_MUSIC) {
            TrackingUtil.setScreen(getApplicationContext(), TrackingUtil.SCREEN_MUSIC);
        } else if (mViewPager.getCurrentItem() == TAB_POSITION_PHOTO) {
            TrackingUtil.setScreen(getApplicationContext(), TrackingUtil.SCREEN_PHOTO);
        } else {
            TrackingUtil.setScreen(getApplicationContext(), TrackingUtil.SCREEN_MEMBERS);
        }
        ConnectionManager.setConnectionManagerListener(this);
    }

    @Override
    public void onStop() {
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareActivity.onStop");
        super.onStop();
        TrackingUtil.stopSession(getApplicationContext(), this);
    }

    @Override
    public void onDestroy() {
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareActivity.onDestroy");
        super.onDestroy();
        closeErrorDialog();
        closeProgressDialog();
        closeEndpartyDialog();
        ConnectionManager.removeConnectionManagerListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mSlideshowItem = menu.findItem(R.id.menu_slideshow);
        mDownloadItem = menu.findItem(R.id.menu_download_mode);
        mSortPhotoItem = menu.findItem(R.id.menu_sort_photo);
        setOptionItemVisisble(mBar.getSelectedNavigationIndex());
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onEndParty();
            break;
        case R.id.menu_sort_photo:
            PhotoEventListener photoEventListener = mPhotoFragment.getPhotoEventListener();
            new SortOrderPhotoDialog(this, photoEventListener).createDialog().show();
            break;
        case R.id.menu_slideshow:
            // file not exists on save folder.
            if (!PhotoUtils.launchSlideshow(this)) {
                Utility.showToast(this,
                        getString(R.string.party_share_strings_slideshow_empty_err_txt),
                        Toast.LENGTH_SHORT);
            }
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void initializeDisplay() {
        if (mConnectionManager != null) {
            if (!mStarted) {
                ConnectionManager.setSessionName(mPartyName);
                mConnectionManager.setUserName(mUserName);
                mConnectionManager.wifiLock();
            }
        }
    }

    private void updateDisplay() {
        if (mConnectionManager != null) {
            updateMemberList(mConnectionManager.getGroupList());
        }
    }

    private void createActionBarCustomView() {
        mBar = getActionBar();
        mBar.setTitle(mPartyName);
        mBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE,
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        mBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME, ActionBar.DISPLAY_SHOW_HOME);
        mBar.setDisplayHomeAsUpEnabled (true);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                mBar.setSelectedNavigationItem(position);
                setOptionItemVisisble(position);
                if (position != TAB_POSITION_PHOTO) {
                    // Dismiss popup menu for when the user
                    // has a flick at the same time as the long tap.
                    if (position == TAB_POSITION_MUSIC) {
                        TrackingUtil.setScreen(getApplicationContext(),
                                TrackingUtil.SCREEN_MUSIC);
                        mMusicFragment.setShowFlag();
                    } else {
                        TrackingUtil.setScreen(getApplicationContext(),
                                TrackingUtil.SCREEN_MEMBERS);
                    }
                    mPhotoFragment.dismissPopupMenu();
                } else {
                    TrackingUtil.setScreen(getApplicationContext(),
                            TrackingUtil.SCREEN_PHOTO);
                }
            }
        });

        MainTabListener listener = new MainTabListener();
        String msg = String.format(getResources().getString(
                R.string.party_share_strings_tab_member_txt), 1);
        View view = getLayoutInflater().inflate(R.layout.action_bar_tab_view, null);
        TextView textView = (TextView) view.findViewById(R.id.tab_name);
        textView.setText(msg);

        mTab = mBar.newTab().setText(msg);
        mTab.setTabListener(listener);
        mBar.addTab(mTab);

        mTab = mBar.newTab().setText(R.string.party_share_strings_tab_music_txt);
        mTab.setTabListener(listener);
        mBar.addTab(mTab);

        mTab = mBar.newTab().setText(R.string.party_share_strings_tab_photo_txt);
        mTab.setTabListener(listener);
        mBar.addTab(mTab);
    }

    private void setOptionItemVisisble(int position) {
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareActivity.setOptionItemVisisble:" + position);
        if (position == TAB_POSITION_PHOTO) {
            if (mDownloadItem != null) {
                mDownloadItem.setVisible(true);
            }
            if (mSortPhotoItem != null) {
                mSortPhotoItem.setVisible(true);
            }
            if (ConnectionManager.isGroupOwner()
                    && PhotoUtils.canLaunchSlideShow(PartyShareActivity.this)) {
                if (mSlideshowItem != null) {
                    mSlideshowItem.setVisible(true);
                }
            } else {
                if (mSlideshowItem != null) {
                    mSlideshowItem.setVisible(false);
                }
            }
        } else {
            if (mSlideshowItem != null) {
                mSlideshowItem.setVisible(false);
            }
            if (mDownloadItem != null) {
                mDownloadItem.setVisible(false);
            }
            if (mSortPhotoItem != null) {
                mSortPhotoItem.setVisible(false);
            }
        }
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

    private void closeEndpartyDialog() {
        if (mEndpartyDialog != null && mEndpartyDialog.isShowing()) {
            mEndpartyDialog.dismiss();
            mEndpartyDialog = null;
        }
    }

    private void setMemberTab() {
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareActivity.setMemberTab");
        int position = mBar.getSelectedNavigationIndex();
        MainTabListener listener = new MainTabListener();
        String msg = String.format(
                getResources().getString(R.string.party_share_strings_tab_member_txt),
                mGroupList.size());
        mTab = mBar.newTab().setText(msg);
        mTab.setTabListener(listener);
        mBar.removeTabAt(0);
        mBar.addTab(mTab, 0);
        mBar.setSelectedNavigationItem(position);
    }

    @Override
    public void connect(WifiP2pConfig config) {
        mStateMachine.sendMessage(WifiDirectStateMachine.CMD_CONNECT, config);
    }

    @Override
    public void disconnect(DeviceInfo deviceInfo) {
        mStateMachine.sendMessage(WifiDirectStateMachine.CMD_NOTIFY_REMOVE_MEMBER, deviceInfo);
    }

    @Override
    public String getDeviceName() {
        return mUserName;
    }

    @Override
    public void setDeviceName(String userName) {
        mUserName = userName;
        mStateMachine.sendMessage(WifiDirectStateMachine.CMD_CHANGE_NAME, mUserName);
    }

    @Override
    public void onEndParty() {
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareActivity.onEndParty");
        final String dialogMessage;
        final String buttonText;
        if (ConnectionManager.isGroupOwner()) {
            dialogMessage = getResources().getString(
                    R.string.party_share_strings_end_party_dialog_txt);
            buttonText = getResources().getString(
                    R.string.party_share_strings_end_party_dialog_button_txt);
        } else {
            dialogMessage = getResources().getString(
                    R.string.party_share_strings_leave_party_dialog_txt);
            buttonText = getResources().getString(
                    R.string.party_share_strings_leave_party_dialog_button_txt);
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_layout_textview, null);
        TextView textView = (TextView)view.findViewById(R.id.dialog_text);
        textView.setText(dialogMessage);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mEndpartyDialog =
                builder.setView(view)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
                        @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (ConnectionManager.isGroupOwner()) {
                                    TrackingUtil.sendShareMusicCount(getApplicationContext());
                                    TrackingUtil.sendSharePhotoCount(getApplicationContext());
                                    TrackingUtil.sendMaxJoinMembers(getApplicationContext());
                                    TrackingUtil.sendPartyTime(getApplicationContext(),
                                            TrackingUtil.ACTION_START_END_TIME);
                                } else {
                                    TrackingUtil.sendPartyTime(getApplicationContext(),
                                            TrackingUtil.ACTION_JOIN_LEAVE_TIME);
                                }

                                mConnectionManager.endParty();
                                mProgressDialog = ProgressDialog.show(PartyShareActivity.this,
                                        null,
                                        getResources().getString(
                                                R.string.party_share_strings_join_waiting_txt),
                                        true, true, null);
                                mProgressDialog.setCanceledOnTouchOutside(false);
                            }
            }).create();
        mEndpartyDialog.setCanceledOnTouchOutside(false);
        mEndpartyDialog.show();
    }

    @Override
    public void updateMemberList(final List<DeviceInfo> groupList) {
        super.updateMemberList(groupList);
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareActivity.updateMemberList");
        if (ConnectionManager.isGroupOwner()) {
            TrackingUtil.compareMaxJoinMembers(groupList.size());
        }
        mGroupList.clear();
        mGroupList.addAll(groupList);
        mWifiDirectFragment.updateGroup(mGroupList);
        setMemberTab();
    }

    @Override
    public void disconnected(String reason) {
        super.disconnected(reason);
        LogUtil.v(LogUtil.LOG_TAG, "PartyShareActivity.disconnected");
        closeProgressDialog();

        if (!mForeground) {
            Intent intent = new Intent();
            intent.setClass(this, StartupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        finish();
    }

    public class MainTabListener implements ActionBar.TabListener {

        @Override
        public void onTabSelected(Tab tab, android.app.FragmentTransaction ft) {
            mViewPager.setCurrentItem(tab.getPosition(), true);
        }

        @Override
        public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) {
            if (tab.getPosition() == TAB_POSITION_MUSIC) {
                mMusicFragment.scrollPlaylist(0);
            } else if (tab.getPosition() == TAB_POSITION_MEMBER) {
                mWifiDirectFragment.scrollList(0);
            } else if (tab.getPosition() == TAB_POSITION_PHOTO) {
                mPhotoFragment.scrollList(0);
            }
        }
    }
}
