package com.specialdark.utopia.shadowtalk.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.specialdark.utopia.shadowtalk.logutil.ShadowTalkLog;

public class BluetoothStateChangeReceiver extends BroadcastReceiver {
    public BluetoothStateChangeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        String action = intent.getAction();
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = BluetoothAdapter.getDefaultAdapter().getState();
            if (state == BluetoothAdapter.STATE_ON) {
                ShadowTalkLog.i("STATE_ON");
            } else if (state == BluetoothAdapter.STATE_OFF) {
                ShadowTalkLog.i("STATE_OFF");

            } else if (state == BluetoothAdapter.STATE_TURNING_ON) {
                ShadowTalkLog.i("STATE_TURNING_ON");

            } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                ShadowTalkLog.i("STATE_TURNING_OFF");

            }
        } else if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {

        } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

        }
        //throw new UnsupportedOperationException("Not yet implemented");
    }
}
