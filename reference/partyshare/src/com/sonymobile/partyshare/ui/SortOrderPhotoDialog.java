/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.util.Setting;

public class SortOrderPhotoDialog {
    private Context mContext;
    private PhotoEventListener mlistener;

    public SortOrderPhotoDialog(Context context) {
        mContext = context;
    }

    public SortOrderPhotoDialog(Context context, PhotoEventListener listener) {
        mContext = context;
        mlistener = listener;
    }

    DialogInterface.OnClickListener mItemListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            Setting.setSortOrderPhoto(mContext, which);
            if (mlistener != null) {
                mlistener.onRefreshList();
            }
            dialog.dismiss();
        }
    };

    public Dialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.party_share_strings_photo_list_sort_by_txt);

        String[] items = {
                mContext.getString(R.string.party_share_strings_date_taken_ascending_txt),
                mContext.getString(R.string.party_share_strings_date_taken_descending_txt),
                mContext.getString(R.string.party_share_strings_date_uploaded_ascending_txt),
                mContext.getString(R.string.party_share_strings_date_uploaded_descending_txt)};
        builder.setSingleChoiceItems(items, Setting.getSortOrderPhoto(mContext), mItemListener);

        Dialog dialog = builder.create();
        return dialog;
    }
}
