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

    private static Set<BluetoothDevice> pairedDevices;

    public static List<String> getPairedDevicesListName() {
        ArrayList<String> list = new ArrayList<>();
        pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                list.add(device.getName());
            }

        } else {
            list.add("No friends found");
        }
        return list;
    }

    public static List<String> getPairedDevicesListAddress() {
        ArrayList<String> list = new ArrayList<>();
        pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                list.add(device.getAddress());
            }

        } else {
            list.add("No Address found");
        }
        return list;
    }

    public static boolean setMyNickName(String name) {

        return mBluetoothAdapter.setName(name);

    }
}
