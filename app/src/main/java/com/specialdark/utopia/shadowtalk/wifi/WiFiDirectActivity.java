package com.specialdark.utopia.shadowtalk.wifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.specialdark.utopia.shadowtalk.R;


public class WiFiDirectActivity extends Activity  implements WifiP2pManager.ChannelListener, DeviceListFragment.DeviceActionListener{

    public WifiP2pManager mWifiP2pManager;
    public WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private boolean mIsWifiP2pEnabled = false;
    private boolean retryChannel = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_main);

        mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(this, getMainLooper(), null/*ChannelListener*/);
        mReceiver = new WiFiDirectBroadcastReceiver(mWifiP2pManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }


    public void setIsWifiP2pEnable(boolean isWifiP2pEnabled) {
        mIsWifiP2pEnabled = isWifiP2pEnabled;
    }
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public void resetDate() {
        DeviceListFragment deviceListFragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
        DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        if (deviceListFragment != null) {
            deviceListFragment.clearPeers();
        }
        if (deviceDetailFragment != null) {
            deviceDetailFragment.resetViews();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.atn_direct_enable:
                if (mWifiP2pManager != null && mChannel != null) {
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    // mChannel and mWifiP2pManager is null.
                }
                return true;

            case R.id.atn_direct_discover:
                if (!mIsWifiP2pEnabled) {
                    Toast.makeText(this,R.string.p2p_off_warning, Toast.LENGTH_LONG).show();
                    return true;
                }
                final DeviceListFragment deviceListFragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
                deviceListFragment.onInitiateDiscovery();
                mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Failed: " + reason, Toast.LENGTH_LONG).show();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onChannelDisconnected() {
        if (mWifiP2pManager != null && !retryChannel) {
            Toast.makeText(WiFiDirectActivity.this,"Channel LOST, trying again", Toast.LENGTH_LONG).show();
            resetDate();
            retryChannel = true;
            mWifiP2pManager.initialize(this,getMainLooper(), this);
        } else {
            Toast.makeText(WiFiDirectActivity.this,"Channel LOST permanently. try re-enable P2P", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        deviceDetailFragment.showDeviceDetail(device);
    }

    @Override
    public void cancelDisconnect() {
        final DeviceListFragment deviceListFragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
        if (deviceListFragment.getDevice() == null || deviceListFragment.getDevice().status == WifiP2pDevice.CONNECTED) {
            disconnect();
        } else if (deviceListFragment.getDevice().status == WifiP2pDevice.AVAILABLE || deviceListFragment.getDevice().status == WifiP2pDevice.INVITED) {
            mWifiP2pManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(WiFiDirectActivity.this,"Aborting connection", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(WiFiDirectActivity.this,"Aborting connection failed, code:" + reason, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void connect(WifiP2pConfig config) {
        mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // our wifi BroadcastReceiver will notify us
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this,"Connect failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void disconnect() {
        final DeviceDetailFragment deviceDetailFragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        deviceDetailFragment.resetViews();
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                deviceDetailFragment.getView().setVisibility(View.GONE);
            }

            @Override
            public void onFailure(int reason) {
                // failed + reason
            }
        });
    }
}
