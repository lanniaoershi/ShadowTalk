/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */

package com.sonymobile.partyshare.ga;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.TagManager;

import com.sonymobile.gagtmhelper.GaGtmExceptionParser;
import com.sonymobile.gagtmhelper.GaGtmLog;
import com.sonymobile.gagtmhelper.GaGtmUtils;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.util.LogUtil;

import java.util.concurrent.TimeUnit;

/**
 * Helper class used to load Google Tag Manager container
 */
public class GtmContainerLoadHelper {

    /** Id for Google Tag Manager container used by PartyShare */
    private static final String GTM_CONTAINER_ID = "GTM-PZ7JXG";

    /** Timeout in milliseconds used when loading Google Tag Manager container */
    private static final long GTM_CONTAINER_LOAD_TIMEOUT_MS = 6000;

    private static final String LOG_TAG = GtmContainerLoadHelper.class.getSimpleName();

    /**
     * Loads the Google Tag Manager container.
     */
    public static void loadContainer(final Context context) {

        LogUtil.d(LOG_TAG, "GtmContainerLoadHelper.loadContainer");

        // Setup Google Analytics through GTM
        // ----------------------------------
        // Debug logging
        GaGtmLog.enable(false);

        // First make sure the SOMC GA setting is complied to
        GaGtmUtils.readAndSetGaEnabled(context);

        TagManager tagManager = TagManager.getInstance(context);
        tagManager.setVerboseLoggingEnabled(false);

        // Make sure only to get the GTM container if GA is enabled. This is
        // added as to avoid data cost of the GTM container.
        if (GaGtmUtils.isSomcGaEnabled(context)) {
            PendingResult<ContainerHolder> pending =
                    tagManager.loadContainerPreferFresh(GTM_CONTAINER_ID,
                            R.raw.gtm_default_container, new Handler(Looper.getMainLooper()));

            pending.setResultCallback(new ResultCallback<ContainerHolder>() {

                @Override
                public void onResult(ContainerHolder containerHolder) {
                    if (!containerHolder.getStatus().isSuccess()) {
                        Log.e(LOG_TAG, "Failure loading container");
                        return;
                    }

                    GaGtmUtils.setContainerHolder(containerHolder);

                    // Push the SOMC defaults like model, build,
                    // customization to the data layer as to enable them to
                    // be used together with GA through GTM
                    // This needs to be done once only
                    GaGtmUtils.pushInitDefaultsToDataLayer(context.getApplicationContext());

                    // Enable advanced exception parsing
                    GaGtmExceptionParser.enableExceptionParsing(context.getApplicationContext());

                    GaGtmUtils.setContainerDefaults(context.getApplicationContext());

                    // Set up a listener to be invoked when a new container
                    // becomes available. This is needed because we can not be sure that the
                    // current container is a network container. The listener will also
                    // be called at later occasions due to automatic or explicit refreshing.
                    containerHolder.setContainerAvailableListener(new ContainerAvailableCallback(
                            context));
                }
            }, GTM_CONTAINER_LOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Used to handle callback when a new container becomes available.
     */
    private static class ContainerAvailableCallback implements
            ContainerHolder.ContainerAvailableListener {

        private final Context mContext;

        private ContainerAvailableCallback(Context context) {
            mContext = context;
        }

        @Override
        public void onContainerAvailable(ContainerHolder containerHolder, String containerVersion) {
            GaGtmUtils.setContainerDefaults(mContext.getApplicationContext());
        }
    }
}
