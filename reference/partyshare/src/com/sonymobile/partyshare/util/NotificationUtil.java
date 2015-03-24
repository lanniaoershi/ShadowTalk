/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.sonymobile.partyshare.R;
import com.sonymobile.partyshare.service.PartyShareService;
import com.sonymobile.partyshare.ui.StartupActivity;


/**
 * Notification utility.
 */
public class NotificationUtil {
    /** Notification ID for join party. */
    public static final int NOTIFICATION_ID_JOIN_PARTY = 1;
    /** Notification ID for end party. */
    public static final int NOTIFICATION_ID_END_PARTY = 2;

    public static void setJoinPartyNotification(
            Context context, String sessionName, boolean isHost) {
        NotificationManager nm =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
        if (isHost) {
            Intent intent = new Intent(context, PartyShareService.class);
            intent.setAction(PartyShareService.ACTION_START_FOREGROUND);
            context.startService(intent);
        } else {
            Notification notification = createJoinPartyNotification(context, sessionName);
            nm.notify(NOTIFICATION_ID_JOIN_PARTY, notification);
        }
    }

    public static void setEndPartyNotification(Context context, String sessionName) {
        NotificationManager nm =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
        Notification notification = createEndPartyNotification(context, sessionName);
        nm.notify(NOTIFICATION_ID_END_PARTY, notification);
    }

    public static void removeJoinPartyNotification(Context context) {
        Intent intent = new Intent(context, PartyShareService.class);
        intent.setAction(PartyShareService.ACTION_STOP_FOREGROUND);
        context.startService(intent);
        caneclJoinPartyNotification(context);
    }

    public static void caneclJoinPartyNotification(Context context) {
        NotificationManager nm =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID_JOIN_PARTY);
    }

    public static Notification createJoinPartyNotification(Context context, String sessionName) {
        Intent intent = new Intent();
        intent.setClass(context, StartupActivity.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setAction(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

        String msg = String.format(
                context.getResources().getString(
                        R.string.party_share_strings_joining_party_notification_txt), sessionName);

        Notification.Builder builder = new Notification.Builder(context);
        builder.setTicker(msg);
        builder.setContentTitle(context.getString(R.string.party_share_strings_app_name_txt));
        builder.setContentText(msg);
        builder.setSmallIcon(R.drawable.party_share_notification_app_icn);
        builder.setAutoCancel(false);
        builder.setContentIntent(contentIntent);
        Notification notification = builder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        return notification;
    }

    public static Notification createEndPartyNotification(Context context, String sessionName) {
        Intent intent = new Intent();
        intent.setClass(context, StartupActivity.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setAction(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

        String msg = String.format(
                context.getResources().getString(
                        R.string.party_share_strings_end_party_notification_txt), sessionName);

        Notification.Builder builder = new Notification.Builder(context);
        builder.setTicker(msg);
        builder.setContentTitle(context.getString(R.string.party_share_strings_app_name_txt));
        builder.setContentText(msg);
        builder.setSmallIcon(R.drawable.party_share_notification_app_icn);
        builder.setAutoCancel(true);
        builder.setContentIntent(contentIntent);
        Notification notification = builder.build();
        return notification;
    }
}
