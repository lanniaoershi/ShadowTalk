/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */

package com.sonymobile.partyshare;

import android.app.Application;

import com.sonymobile.partyshare.ga.GtmContainerLoadHelper;
import com.sonymobile.partyshare.ga.TrackingUtil;
import com.sonymobile.partyshare.util.Utility;

/**
 * Application class that loads Google Tag Manager container
 */
public class PartyShareApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        GtmContainerLoadHelper.loadContainer(this);

        String isXperia = TrackingUtil.LABEL_OTHER;
        if (Utility.hasHostCapability(getBaseContext())) {
            isXperia = TrackingUtil.LABEL_XPERIA;
        }
        TrackingUtil.sendEvent(getApplicationContext(), TrackingUtil.CATEGORY_USER,
                TrackingUtil.ACTION_START_ON_XPERIA, isXperia, TrackingUtil.VALUE_COUNT);
    }
}
