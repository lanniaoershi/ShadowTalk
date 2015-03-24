/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.ListFragment;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.session.WifiP2pSessionInfo;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple ListFragment that shows the available services as published by the
 * peers
 */
public class SessionFragment extends ListFragment {

    WifiDevicesAdapter mListAdapter = null;
    private List<WifiP2pSessionInfo> mSessionList =
            Collections.synchronizedList(new ArrayList<WifiP2pSessionInfo>());

    interface DeviceClickListener {
        public void connectP2p(WifiP2pSessionInfo wifiP2pService);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.session_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListAdapter = new WifiDevicesAdapter(this.getActivity(),
                R.layout.session_list_row, android.R.id.text1,
                mSessionList);
        setListAdapter(mListAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ((DeviceClickListener) getActivity()).connectP2p(
                (WifiP2pSessionInfo) l.getItemAtPosition(position));
    }

    public void updateSessionList(List<WifiP2pSessionInfo> sessionList) {
        mSessionList.clear();
        mSessionList.addAll(sessionList);
        if (mListAdapter != null) {
            mListAdapter.notifyDataSetChanged();
        }
    }

    public void clearSessionList() {
        mSessionList.clear();
        if (mListAdapter != null) {
            mListAdapter.notifyDataSetChanged();
        }
    }

    public class WifiDevicesAdapter extends ArrayAdapter<WifiP2pSessionInfo> {
        private Context mContext;
        private List<WifiP2pSessionInfo> items;

        public WifiDevicesAdapter(Context context, int resource, int textViewResourceId,
                List<WifiP2pSessionInfo> items) {
            super(context, resource, textViewResourceId, items);
            this.mContext = context;
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi =
                        (LayoutInflater) getActivity().getSystemService(
                                Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.session_list_row, null);
            }
            WifiP2pSessionInfo sessionInfo = items.get(position);
            if (sessionInfo != null) {
                LinearLayout layout = (LinearLayout) v.findViewById(R.id.session_layout);
                int resId = Utility.getSessionListResId(
                        mContext, sessionInfo.sessionName, sessionInfo.userName);
                layout.setBackgroundResource(resId);

                TextView partyText = (TextView) v.findViewById(android.R.id.text1);
                TextView userText = (TextView) v.findViewById(android.R.id.text2);
                TextView timeText = (TextView) v.findViewById(R.id.text3);

                if (partyText != null) {
                    partyText.setText(sessionInfo.sessionName);
                }

                if (userText != null) {
                    String userInfo = String.format(getResources().getString(
                            R.string.party_share_strings_startup_host_by_txt),
                            sessionInfo.userName);
                    userText.setText(userInfo);
                }

                if (timeText != null) {
                    String hour = "";
                    String minute = "";
                    String time = sessionInfo.time;
                    if (time != null) {
                        hour = time.substring(8, 10);
                        minute = time.substring(10, 12);
                    }
                    String timeInfo = "";
                    try {
                        timeInfo = String.format(getResources().getString(
                                R.string.party_share_strings_startup_starttime_txt),
                                Integer.parseInt(hour),
                                Integer.parseInt(minute));
                    } catch (NumberFormatException e) {
                        LogUtil.e(LogUtil.LOG_TAG, "NumberFormatException : " + e);
                        timeInfo = String.format(getResources().getString(
                                R.string.party_share_strings_startup_starttime_txt),
                                00, 00);
                    }
                    timeText.setText(timeInfo);
                }
            }
            return v;
        }
    }

    public static String getDeviceStatus(int statusCode) {
        switch (statusCode) {
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }
}
