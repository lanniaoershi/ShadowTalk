package com.specialdark.utopia.shadowtalk;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.specialdark.utopia.shadowtalk.bluetooth.BluetoothChatService;
import com.specialdark.utopia.shadowtalk.bluetooth.BluetoothDeviceListActivity;
import com.specialdark.utopia.shadowtalk.bluetooth.util.GetBondedBluetoothDevices;
import com.specialdark.utopia.shadowtalk.constant.ShadowTalkConstant;
import com.specialdark.utopia.shadowtalk.logutil.ShadowTalkLog;

public class ShadowTalkActivity extends FragmentActivity implements MyInfoFragment.OnFragmentInteractionListener{


    private ViewPager mViewPager;
    private static BluetoothAdapter mBluetoothAdapter;
    private static int mState;
    private BluetoothChatService mChatServiceAtMainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shadow_talk);


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        ImageButton mFriendsImageButton = (ImageButton) findViewById(R.id.btn_friends_page);
        ImageButton mGroupsImageButton = (ImageButton) findViewById(R.id.btn_groups_page);
        ImageButton mSettingsImageButton = (ImageButton) findViewById(R.id.btn_settings_page);
        ImageButton mAddFriendImageButton = (ImageButton) findViewById(R.id.add_friends);
        ImageButton mMyInfoImageButton = (ImageButton) findViewById(R.id.myself_icon);

        mFriendsImageButton.setOnClickListener(new BottomBtnOnClickListener(0));
        mGroupsImageButton.setOnClickListener(new BottomBtnOnClickListener(1));
        mSettingsImageButton.setOnClickListener(new BottomBtnOnClickListener(2));
        mAddFriendImageButton.setOnClickListener(new AddFriendsBtnOnClickListener());
        mMyInfoImageButton.setOnClickListener(new MyInfoBtnOnClickListener());

    }

    @Override
    protected void onStart() {
        super.onStart();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            // change bluetooth icon or sth
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, ShadowTalkConstant.REQUEST_ENABLE_BT);
        }
//        mChatServiceAtMainActivity = new BluetoothChatService(this, mHandlerMainActivity, null);
//        mChatServiceAtMainActivity.start();

    }

    public static int getState() {
        return mState;
    }

    public static void setState(int state) {
        mState = state;
    }

    public static final Handler mHandlerMainActivity = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ShadowTalkLog.i("state = "+msg.what);
            switch (msg.what) {
                case ShadowTalkConstant.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ShadowTalkConstant.STATE_CONNECTED:
//                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
//                            mConversationArrayAdapter.clear();
                            setState(ShadowTalkConstant.STATE_CONNECTED);
                            break;
                        case ShadowTalkConstant.STATE_CONNECTING:
                            setState(ShadowTalkConstant.STATE_CONNECTING);
//                            setStatus(R.string.title_connecting);
                            break;
                        case ShadowTalkConstant.STATE_LISTEN:
                        case ShadowTalkConstant.STATE_NONE:
//                            setStatus(R.string.title_not_connected);
                            setState(ShadowTalkConstant.STATE_NONE);
                            break;
                    }
                    break;
                case ShadowTalkConstant.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
//                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case ShadowTalkConstant.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
//                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case ShadowTalkConstant.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
//                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
//                    if (null != activity) {
//                        Toast.makeText(activity, "Connected to "
//                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
//                    }
                    break;
                case ShadowTalkConstant.MESSAGE_TOAST:
//                    if (null != activity) {
//                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
//                                Toast.LENGTH_SHORT).show();
//                    }
                    break;
            }
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ShadowTalkConstant.REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // change bluetooth icon or sth

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, R.string.bt_not_enabled_retry_on_settings, Toast.LENGTH_SHORT).show();
                    // finish();
                }
                break;
            case ShadowTalkConstant.REQUEST_CONNECT_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras()
                            .getString(BluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

                    device.createBond();
                    //Use reflect to use hide method cancelPairingUserInput to implement auto pair with random pin code
//                    try {
//
//                        ShadowTalkLog.i("cancelPairingUserInput");
//                        Method cancelPairingUserInput = BluetoothDevice.class.getMethod("cancelPairingUserInput", (Class[]) null);
//                        cancelPairingUserInput.invoke(device, (Object[]) null);
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        ShadowTalkLog.i("catch sth");
//                    }
                    //API>19 already provide method createBond so don't need to use reflect to invoke method
//                    try {
//                        Method createBond = BluetoothDevice.class.getMethod("createBond", (Class[]) null);
//                        createBond.invoke(device, (Object[]) null);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    device.createBond();
                }
                break;
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        MyInfoFragment myInfoFragment = MyInfoFragment.newInstance(null,null);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_replace, myInfoFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();

    }

    public class BottomBtnOnClickListener implements View.OnClickListener {

        private int index = 0;

        public BottomBtnOnClickListener(int i) {
            index = i;
        }

        @Override
        public void onClick(View v) {

            mViewPager.setCurrentItem(index);

        }
    }

    public class AddFriendsBtnOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(ShadowTalkConstant.ACTION_BLUETOORH_DEVICE_LIST_ACTIVITY);
            startActivityForResult(intent, ShadowTalkConstant.REQUEST_CONNECT_DEVICE_SECURE);
        }
    }

    public class MyInfoBtnOnClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            onFragmentInteraction(null);
        }
    }



    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return FriendsFragment.newInstance(position + 1);
                case 1:
                    return GroupsFragment.newInstance(position + 1);
                case 2:
                    return SettingsFragment.newInstance(position + 1);
                default:
                    return FriendsFragment.newInstance(position + 1);
            }
            // getItem is called to instantiate the fragment for the given page.
            // Return a Fragment (defined as a static inner class below).

        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }


    public static class FriendsFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public static FriendsFragment newInstance(int sectionNumber) {
            FriendsFragment fragment = new FriendsFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public FriendsFragment() {
        }

        public class FriendsAdapter extends BaseAdapter {

            private List<String> friendsList = new ArrayList<>();
            private Context context;

            public FriendsAdapter(Context context, List<String> friendsList) {
                this.context = context;
                this.friendsList = friendsList;
            }

            @Override
            public int getCount() {
                return friendsList.size();
            }

            @Override
            public Object getItem(int position) {
                return friendsList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            public String getNameByPosition(int position) {
                return friendsList.get(position);
            }


            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                convertView = LayoutInflater.from(context).inflate(R.layout.row_friends, null);

                TextView friendName = (TextView) convertView.findViewById(R.id.friend_name);
                TextView friendConversations = (TextView) convertView.findViewById(R.id.friend_conversation);
                ImageView friendIconBmp = (ImageView) convertView.findViewById(R.id.friend_icon);
                ImageView friendStatusBmp = (ImageView) convertView.findViewById(R.id.friend_statue);

                friendName.setText(friendsList.get(position));
                friendConversations.setText("");
                friendIconBmp.setImageDrawable(getResources().getDrawable(R.mipmap.ic_launcher));
                friendStatusBmp.setImageDrawable(getResources().getDrawable(R.mipmap.ic_launcher));

                return convertView;
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_friends_page, container, false);
            ListView pairedFriendsListView = (ListView) rootView.findViewById(R.id.list_view_friends);
            final FriendsAdapter pairedDevicesArrayAdapter = new FriendsAdapter(getActivity(), GetBondedBluetoothDevices.getPairedDevicesListName());
            pairedFriendsListView.setAdapter(pairedDevicesArrayAdapter);

            pairedFriendsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    Intent intent = new Intent(ShadowTalkConstant.ACTION_BLUETOOTH_CHAT_ACTIVITY);
                    intent.putExtra("NAME", pairedDevicesArrayAdapter.getNameByPosition(position));
                    List<String> listAddress = GetBondedBluetoothDevices.getPairedDevicesListAddress();
                    String address = listAddress.get(position);
                    intent.putExtra("device_address", address);
                    getActivity().setResult(Activity.RESULT_OK, intent);
                    startActivity(intent);

                }
            });


            return rootView;
        }
    }

    public static class GroupsFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static GroupsFragment newInstance(int sectionNumber) {
            GroupsFragment fragment = new GroupsFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public GroupsFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_groups_page, container, false);
            return rootView;
        }
    }

    public static class SettingsFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static SettingsFragment newInstance(int sectionNumber) {
            SettingsFragment fragment = new SettingsFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public SettingsFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_settings_page, container, false);
            Switch wifiSwitch = (Switch) rootView.findViewById(R.id.switch_wifi);
            Switch bluetoothSwitch = (Switch) rootView.findViewById(R.id.switch_bluetooth);
            bluetoothSwitch.setChecked(mBluetoothAdapter.isEnabled());

//            BluetoothStateChangeReceiver bluetoothStateChangeReceiver = new BluetoothStateChangeReceiver();
//            IntentFilter intentFilter = new IntentFilter();
//            intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
//            getActivity().registerReceiver(bluetoothStateChangeReceiver,intentFilter);


            bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mBluetoothAdapter.enable();
                    } else
                        mBluetoothAdapter.disable();

                }
            });


            return rootView;
        }
    }

}
