package com.specialdark.utopia.shadowtalk.bluetooth.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.specialdark.utopia.shadowtalk.logutil.ShadowTalkLog;

import java.util.ArrayList;

import java.util.List;
import java.util.Set;

/**
 * Created by weiwei on 4/1/15.
 */
public class GetBondedBluetoothDevices {

    private static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private static  Set<BluetoothDevice> pairedDevices;

    public static List<String> getPairedDevicesList() {
        ArrayList<String> list = new ArrayList<>();
        pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            ShadowTalkLog.i("pairedDevices = "+pairedDevices.size());
            for (BluetoothDevice device : pairedDevices) {
                list.add(device.getName());
            }

        } else {
            list.add("No friends found");
        }
        return list;
    }
}
