/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.service.PhotoDownloadService;
import com.sonymobile.partyshare.util.Setting;

public class DownloadModeDialog {
    private Context mContext;

    public DownloadModeDialog(Context context) {
        mContext = context;
    }

    DialogInterface.OnClickListener mItemListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            Setting.setDownloadMode(mContext, which);

            // Start/Stop auto download.
            String action = PhotoDownloadService.ACTION_STOP_AUTO_DL;
            if (which == Setting.PHOTO_DOWNLOAD_MODE_AUTO) {
                action = PhotoDownloadService.ACTION_START_AUTO_DL;
            } else if (which == Setting.PHOTO_DOWNLOAD_MODE_MANUAL) {
                action = PhotoDownloadService.ACTION_STOP_AUTO_DL;
            }
            Intent intent = new Intent(action);
            mContext.startService(intent);

            dialog.dismiss();
        }
    };

    public Dialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.party_share_strings_photo_list_download_setting_txt);

        String[] items = {
                mContext.getString(R.string.party_share_strings_photo_list_auto_txt),
                mContext.getString(R.string.party_share_strings_photo_list_manual_txt)};
        builder.setSingleChoiceItems(items, Setting.getDownloadMode(mContext), mItemListener);

        Dialog dialog = builder.create();
        return dialog;
    }
}
