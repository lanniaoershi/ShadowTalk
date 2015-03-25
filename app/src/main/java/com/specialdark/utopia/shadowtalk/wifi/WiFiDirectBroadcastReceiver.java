package com.specialdark.utopia.shadowtalk.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

import com.specialdark.utopia.shadowtalk.R;

/**
 * Created by weiwei on 3/24/15.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver{

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private WiFiDirectActivity mWiFiDirectActivity;


    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       WiFiDirectActivity activity) {
        super();
        this.mWifiP2pManager = manager;
        this.mChannel = channel;
        this.mWiFiDirectActivity = activity;

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enable anf notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                mWiFiDirectActivity.setIsWifiP2pEnable(true);
            } else {
                // Wifi P2P is disabled
                mWiFiDirectActivity.setIsWifiP2pEnable(true);
                mWiFiDirectActivity.resetDate();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list if current peers
            if (mWifiP2pManager != null) {
                mWifiP2pManager.requestPeers(mChannel, (WifiP2pManager.PeerListListener) mWiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_list));
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            //Respond to new connection or disconnections
            if (mWifiP2pManager == null) {
                return;
            }
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment) mWiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_detail);
                mWifiP2pManager.requestConnectionInfo(mChannel, deviceDetailFragment);
            } else {
                mWiFiDirectActivity.resetDate();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            DeviceListFragment deviceListFragment = (DeviceListFragment) mWiFiDirectActivity.getFragmentManager().findFragmentById(R.id.frag_list);
            deviceListFragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        }
    }
}
