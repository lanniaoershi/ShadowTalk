/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * All rights, including trade secret rights, reserved.
 */
package com.sonymobile.partyshare.httpd;

/**
 * PartyShareEvent definition.
 * These events are sent to client from Host.
 */
public class PartyShareEvent {
    /** Parameter of event code. */
    public static final String PARAM_EVENT_CODE = "event_code";
    /** Event of add content. */
    public static final int EVENT_CONTENT_ADDED = 0;
    /** Event of remove content. */
    public static final int EVENT_CONTENT_REMOVED = 1;
    /** Event of play next. */
    public static final int EVENT_PLAY_NEXT = 2;
    /** Event of load playlist. */
    public static final int EVENT_LOAD_PLAYLIST = 3;
    /** Event of load photolist. */
    public static final int EVENT_LOAD_PHOTOLIST = 4;
    /** Event of remove music. */
    public static final int EVENT_REMOVE_LOCAL_MUSIC = 5;
    /** Event of remove photo. */
    public static final int EVENT_REMOVE_PHOTO = 6;
    /** Event of get file size. */
    public static final int EVENT_GET_FILESIZE = 7;
    /** Event of remove local photo. */
    public static final int EVENT_REMOVE_LOCAL_PHOTO = 8;
    /** Event of no data. */
    public static final int EVENT_DELETE_CLIENT_DATA = 9;
    /** Event of join. */
    public static final int EVENT_JOIN = 10;
}
