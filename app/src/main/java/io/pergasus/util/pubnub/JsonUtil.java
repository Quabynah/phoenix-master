/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.pergasus.util.pubnub;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Utility class for converting JSON objects/strings/etc.
 */
public class JsonUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> T fromJson(byte[] value, Class<T> clazz) throws Exception {
        return mapper.readValue(value, clazz);
    }

    public static <T> T fromJson(String value, Class<T> clazz) throws Exception {
        return mapper.readValue(value, clazz);
    }

    public static String asJson(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    public static <T> T convert(Map<String, Object> value, Class<T> clazz) throws Exception {
        return mapper.convertValue(value, clazz);
    }
}
