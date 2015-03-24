/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pConfig;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.httpd.MusicPlayListController;
import com.sonymobile.partyshare.session.ConnectionManager;
import com.sonymobile.partyshare.session.DeviceInfo;
import com.sonymobile.partyshare.util.LogUtil;
import com.sonymobile.partyshare.util.Setting;


/**
 * The list of members who have participated in Party.
 */
public class MemberFragment extends BaseFragment implements OnItemClickListener {

    private WifiPeerListAdapter mListAdapter = null;
    private ListView mListView;
    private List<DeviceInfo> mMemberList = new ArrayList<DeviceInfo>();
    private PopupMenu mPopup;
    private TextView mNoGuestTextView;
    private static boolean mShowDialog = false;

    public MemberFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        LogUtil.v(LogUtil.LOG_TAG, "MemberFragment.onCreateView");
        View view = getActivity().getLayoutInflater().inflate(R.layout.member_fragment, null);
        mNoGuestTextView = (TextView)view.findViewById(R.id.no_guest_text);
        Button partyEnd = (Button)view.findViewById(R.id.party_end);
        if (ConnectionManager.isGroupOwner()) {
            if (mMemberList.size() <= 1) {
                mNoGuestTextView.setVisibility(View.VISIBLE);
            } else {
                mNoGuestTextView.setVisibility(View.INVISIBLE);
            }
            partyEnd.setText(R.string.party_share_strings_member_list_end_party_button_txt);
        } else {
            mNoGuestTextView.setVisibility(View.INVISIBLE);
            partyEnd.setText(R.string.party_share_strings_leave_party_button_txt);
        }
        partyEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((IWifiDirectFragmentActionListener) getActivity()).onEndParty();
            }
        });

        mListView = (ListView)view.findViewById(R.id.member_list_view);
        mListAdapter = new WifiPeerListAdapter(getActivity(),
                R.layout.member_list_row, mMemberList);
        mMemberList.clear();
        mMemberList.addAll(((DeviceListActionListener)getActivity()).getGroupList());
        mListView.addFooterView(getFooter(), null, true);
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(this);

        return view;
    }

    private View getFooter() {
        LayoutInflater vi =
                (LayoutInflater)getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.member_list_row_footer, null);
        return v;
    }

    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private class WifiPeerListAdapter extends ArrayAdapter<DeviceInfo> {

        private List<DeviceInfo> items;

        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public WifiPeerListAdapter(Context context, int textViewResourceId,
                List<DeviceInfo> objects) {
            super(context, textViewResourceId, objects);
            items = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi =
                        (LayoutInflater)getActivity().getSystemService(
                                Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.member_list_row, null);
            }

            if (mListView.getCount() <= position) {
                return getFooter();
            }

            final DeviceInfo info = items.get(position);
            if (info != null) {
                TextView nameView = (TextView)v.findViewById(R.id.user_name);
                TextView statusView = (TextView)v.findViewById(R.id.user_status);
                ImageView image = (ImageView)v.findViewById(R.id.user_image);

                if (nameView == null || statusView == null || image == null) {
                    return v;
                }

                String username = info.getUserName();
                nameView.setText(username);

                String address = ConnectionManager.getMyAddress();
                if (!address.isEmpty()) {
                    String status = "";
                    statusView.setVisibility(View.VISIBLE);
                    if (address.equals(info.getDeviceAddress())) {
                        if (info.getIsHost()) {
                            status = getResources().getString(
                                    R.string.party_share_strings_member_list_me_host_txt);
                            image.setImageResource(
                                    R.drawable.party_share_member_list_person_host_icn);
                        } else {
                            status = getResources().getString(
                                    R.string.party_share_strings_member_list_me_guest_txt);
                            image.setImageResource(
                                    R.drawable.party_share_member_list_person_guest_icn);
                        }
                        nameView.setTextColor(
                                getResources().getColor(R.color.main_focus_text_color));
                        statusView.setTextColor(
                                getResources().getColor(R.color.sub_focus_text_color));
                    } else {
                        if (info.getIsHost()) {
                            status = getResources().getString(
                                    R.string.party_share_strings_member_list_host_txt);
                            image.setImageResource(
                                    R.drawable.party_share_member_list_person_host_icn);
                        } else {
                            statusView.setVisibility(View.GONE);
                            image.setImageResource(
                                    R.drawable.party_share_member_list_person_guest_icn);
                        }
                        nameView.setTextColor(
                                getResources().getColor(R.color.main_text_color));
                        statusView.setTextColor(
                                getResources().getColor(R.color.sub_text_color));
                    }
                    statusView.setText(status);
                }
            }
            return v;
        }
    }

    public synchronized void updateGroup(List<DeviceInfo> groupList) {
        if (mPopup != null) {
            mPopup.dismiss();
            mPopup = null;
        }
        mMemberList.clear();
        mMemberList.addAll(groupList);
        if (mListAdapter != null) {
            if (ConnectionManager.isGroupOwner()) {
                if (mMemberList.size() <= 1) {
                    mNoGuestTextView.setVisibility(View.VISIBLE);
                } else {
                    mNoGuestTextView.setVisibility(View.INVISIBLE);
                }
            }
            mListAdapter.notifyDataSetChanged();
        }
    }

    public void clearPeers() {
        if (mPopup != null) {
            mPopup.dismiss();
            mPopup = null;
        }
        mMemberList.clear();
        if (mListAdapter != null) {
            mListAdapter.notifyDataSetChanged();
        }
    }

    public void scrollList(final int position) {
        mListView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mListView.setSelection(position);
            }
        }, 100);
    }

    public static class EditDialogFragment extends DialogFragment {
        private static String mMyName;

        public static void setMyName(String name) {
            mMyName = name;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            LayoutInflater inflater =
                (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.dialog_layout_username, null);
            final EditText editText = (EditText)view.findViewById(R.id.edit);

            final AlertDialog dialog =
                builder.setView(view)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Context context = getActivity();
                                    String username = editText.getText().toString();
                                    if (!mMyName.equals(username)) {
                                        ((DeviceListActionListener)context).setDeviceName(username);
                                        Setting.setUserName(context, username);
                                        String address = ConnectionManager.getMyAddress();
                                        MusicPlayListController.changeName(
                                                context, address, username);
                                    }
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();

            editText.setText(mMyName);
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
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                    } else {
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    }
                }
            });

            return dialog;
        }

        @Override
        public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            setShowDialog(false);
        }
    }

    public static class ConfirmationDialogFragment extends DialogFragment {
        private static String mName;
        private static DeviceInfo mDevice;

        public static void setName(String name) {
            mName = name;
        }

        public static void setDeviceInfo(DeviceInfo device) {
            mDevice = device;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

             final AlertDialog dialog =
                builder.setMessage(String.format(getResources().getString(
                        R.string.party_share_strings_remove_member_dialog_txt), mName))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(getResources().getString(
                        R.string.party_share_strings_remove_dialog_button_txt),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ((DeviceListActionListener)getActivity())
                                            .disconnect(mDevice);
                                }
                            })
                    .create();
            return dialog;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if ((mListView.getCount() - 1) == position) {
            Intent intent = new Intent(getActivity(), HelpActivity.class);
            intent.putExtra(HelpActivity.EXTRA_SHOW_TOP, false);
            startActivity(intent);
        } else {
            ListView listView = (ListView) parent;
            final DeviceInfo deviceInfo = (DeviceInfo) listView.getItemAtPosition(position);
            if (deviceInfo == null) {
                LogUtil.e(LogUtil.LOG_TAG, "MemberFragmen.onItemClick deviceInfo is null.");
                return;
            }
            String address = ConnectionManager.getMyAddress();
            final String username = deviceInfo.getUserName();
            if (address.isEmpty()) {
                LogUtil.e(LogUtil.LOG_TAG,
                        "MemberFragmen.onItemClick device or deviceAddress is null.");
                return;
            }
            if (address.equals(deviceInfo.getDeviceAddress())) {
                if (!isShowDialog()) {
                    setShowDialog(true);
                    EditDialogFragment.setMyName(username);
                    EditDialogFragment editDialogFragment = new EditDialogFragment();
                    editDialogFragment.show(getFragmentManager(), "edit_name");
                }
            } else {
                if (ConnectionManager.isGroupOwner()) {
                    if (mPopup != null) {
                        mPopup.dismiss();
                        mPopup = null;
                    }
                    mPopup = new PopupMenu(getActivity(), view);
                    mPopup.getMenuInflater().inflate(R.menu.member_list_popup, mPopup.getMenu());
                    mPopup.show();
                    mPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            ConfirmationDialogFragment.setDeviceInfo(deviceInfo);
                            ConfirmationDialogFragment.setName(username);
                            ConfirmationDialogFragment dialog = new ConfirmationDialogFragment();
                            dialog.show(getFragmentManager(), "conf_name");
                            return true;
                        }
                    });
                }
            }
        }
    }

    private static boolean isShowDialog() {
        return mShowDialog;
    }

    private static void setShowDialog(boolean showDialog) {
        mShowDialog = showDialog;
    }

    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
    public interface IWifiDirectFragmentActionListener {
        void onEndParty();
    }

    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
    public interface DeviceListActionListener {

        void connect(WifiP2pConfig config);

        void disconnect(DeviceInfo deviceInfo);

        String getDeviceName();

        void setDeviceName(String deviceName);

        List<DeviceInfo> getGroupList();
    }
}
