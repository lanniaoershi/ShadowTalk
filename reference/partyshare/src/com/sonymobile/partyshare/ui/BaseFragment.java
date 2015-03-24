/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.ui;

import android.support.v4.app.Fragment;

public abstract class BaseFragment extends Fragment {
    public static final String KEY_TITLE = "title";

    public BaseFragment() {
        super();
    }

    public String getTitle() {
        return getArguments().getString(KEY_TITLE);
    }
}
