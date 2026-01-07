package com.example.Pickbyvision.Induvidual_Pick.network;

import com.example.Pickbyvision.BuildConfig;

public final class ApiConfig {

    private ApiConfig() {}

    public static final String BASE_URL = BuildConfig.API_BASE_URL;
    private static final String PICK_PREFIX = "PICK_/";
    public static final String HEADER_AUTH = "Authorization";


    public static final String HEADER_API_KEY = "XApiKey";
    public static final String API_KEY = BuildConfig.VENDOR_API_KEY;


    public static final String CHECK_LOGIN       = BASE_URL + PICK_PREFIX + "CHECK_LOGIN";
    public static final String PICKLIST_JOB      = BASE_URL + PICK_PREFIX + "PICKLIST_JOB";
    public static final String ORDER_DETAILS     = BASE_URL + PICK_PREFIX + "ORDER_DETAILS";
    public static final String CONFIRM_ORDER     = BASE_URL + PICK_PREFIX + "confirm_order";
    public static final String ORDER_SUMMARY     = BASE_URL + PICK_PREFIX + "ORDER_SUMMARY";
    public static final String START_JOB_PICKING = BASE_URL + PICK_PREFIX + "START_JOB_PICKING";
    public static final String PAUSE_JOB_PICKING = BASE_URL + PICK_PREFIX + "PAUSE_JOB_PICKING";
    public static final String RESUME_JOB_PICKING= BASE_URL + PICK_PREFIX + "RESUME_JOB_PICKING";
    public static final String GET_OUTBOUND_ALLOTED_TIME = BASE_URL + PICK_PREFIX + "GET_OUTBOUND_ALLOTED_TIME";
    public static final String GET_JOB_TIMING    = BASE_URL + PICK_PREFIX + "GET_JOB_TIMING";


    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_USER_AGENT = "User-Agent";

    public static final String ACCEPT_ALL = "*/*";
    public static final String ACCEPT_JSON = "application/json";
    public static final String USER_AGENT_VALUE = "PickByVision/1.0";

}
