package com.specialdark.utopia.shadowtalk.wifi;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


import com.specialdark.utopia.shadowtalk.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by weiwei on 3/24/15.
 *
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events.
 *
 */


public class DeviceListFragment extends ListFragment implements WifiP2pManager.PeerListListener{

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    public ProgressDialog mProgressDialog = null;
    public View mContentView = null;
    private WifiP2pDevice mDevice;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.row_devices, peers));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list, null);
        return mContentView;
    }

    public WifiP2pDevice getDevice() {
        return mDevice;
    }

    public static String getDeviceStatue(int deviceStatue) {
        switch (deviceStatue) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);
        ((DeviceActionListener) getActivity()).showDetails(device);
        //super.onListItemClick(l, v, position, id);
    }

    public interface DeviceActionListener {
        void showDetails(WifiP2pDevice device);
        void cancelDisconnect();
        void connect(WifiP2pConfig config);
        void disconnect();
    }

    public void updateThisDevice(WifiP2pDevice device) {
        mDevice = device;
        TextView textView = (TextView) mContentView.findViewById(R.id.my_name);
        textView.setText(device.deviceName);
        textView = (TextView) mContentView.findViewById(R.id.my_status);
        textView.setText(getDeviceStatue(device.status));
    }
    public void onInitiateDiscovery() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "Finding peers", true, true, new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {

            }
        });
    }

    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

        private List<WifiP2pDevice> items;

        public WiFiPeerListAdapter(Context context, int resource, List<WifiP2pDevice> objects) {
            super(context, resource, objects);
            items = objects;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater.inflate(R.layout.row_devices, null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null) {
                TextView textViewDeviceName = (TextView) view.findViewById(R.id.device_name);
                TextView textViewDeviceDetails = (TextView) view.findViewById(R.id.device_details);
                if (textViewDeviceName != null) {
                    textViewDeviceName.setText(device.deviceName);
                }
                if (textViewDeviceDetails != null) {
                    textViewDeviceDetails.setText(getDeviceStatue(device.status));
                }
            }
            return view;
        }
    }





    @Override
    public void onPeersAvailable(WifiP2pDeviceList peersList) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        peers.clear();
        peers.addAll(peersList.getDeviceList());
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
            // can Log.w() here, No device found.
            return;
        }
    }

    public void clearPeers() {
        peers.clear();
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }
}
