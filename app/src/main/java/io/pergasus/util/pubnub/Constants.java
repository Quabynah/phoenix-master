/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util.pubnub;

import io.pergasus.BuildConfig;

public class Constants {
	public static final String PUBNUB_PUBLISH_KEY = BuildConfig.PUBNUB_PUBLISH_KEY;
	public static final String PUBNUB_SUBSCRIBE_KEY = BuildConfig.PUBNUB_SUBSCRIBE_KEY;
	
	public static final String SUBSCRIBE_CHANNEL_NAME = "SUBSCRIBE_CHANNEL_NAME";
	public static final String PUBLISH_CHANNEL_NAME = "PUBLISH_CHANNEL_NAME";
	public static final String FLIGHTPATHS_CHANNEL_NAME = "FLIGHTPATHS_CHANNEL_NAME";
	
	public static final String DATASTREAM_PREFS = "DATASTREAM_PREFS";
	public static final String DATASTREAM_UUID = "DATASTREAM_UUID";
}
