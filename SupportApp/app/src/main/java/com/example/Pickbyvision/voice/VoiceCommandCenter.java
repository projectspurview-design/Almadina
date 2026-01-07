package com.example.Pickbyvision.voice;

import android.app.Activity;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;


public final class VoiceCommandCenter {

    private static final String TAG = "VoiceCommandCenter";


    public static final int KEY_NEXT          = KeyEvent.KEYCODE_F1;
    public static final int KEY_SCROLL_UP     = KeyEvent.KEYCODE_F2;
    public static final int KEY_SCROLL_DOWN   = KeyEvent.KEYCODE_F3;
    public static final int KEY_SELECT        = KeyEvent.KEYCODE_F4;
    public static final int KEY_INBOUND       = KeyEvent.KEYCODE_F5;
    public static final int KEY_OUTBOUND      = KeyEvent.KEYCODE_F6;
    public static final int KEY_INVENTORY     = KeyEvent.KEYCODE_F7;
    public static final int KEY_LOGOUT        = KeyEvent.KEYCODE_F8;
    public static final int KEY_BACK          = KeyEvent.KEYCODE_F9;
    public static final int KEY_INDIVIDUAL    = KeyEvent.KEYCODE_F10;
    public static final int KEY_CONSOLIDATED  = KeyEvent.KEYCODE_F11;


    public static final int KEY_SCAN         = KeyEvent.KEYCODE_BUTTON_7;
    public static final int KEY_SHORT         = KeyEvent.KEYCODE_BUTTON_1;
    public static final int KEY_JUMP_TO       = KeyEvent.KEYCODE_BUTTON_2;
    public static final int KEY_SHORT_REMOVE  = KeyEvent.KEYCODE_BUTTON_3;
    public static final int KEY_SHORT_ADD     = KeyEvent.KEYCODE_BUTTON_4;
    public static final int KEY_SHORT_BACK    = KeyEvent.KEYCODE_BUTTON_5;
    public static final int KEY_SHORT_NEXT    = KeyEvent.KEYCODE_BUTTON_6;


    public static final int KEY_DIGIT_0       = KeyEvent.KEYCODE_0;
    public static final int KEY_DIGIT_1       = KeyEvent.KEYCODE_1;
    public static final int KEY_DIGIT_2       = KeyEvent.KEYCODE_2;
    public static final int KEY_DIGIT_3       = KeyEvent.KEYCODE_3;
    public static final int KEY_DIGIT_4       = KeyEvent.KEYCODE_4;
    public static final int KEY_DIGIT_5       = KeyEvent.KEYCODE_5;
    public static final int KEY_DIGIT_6       = KeyEvent.KEYCODE_6;
    public static final int KEY_DIGIT_7       = KeyEvent.KEYCODE_7;
    public static final int KEY_DIGIT_8       = KeyEvent.KEYCODE_8;
    public static final int KEY_DIGIT_9       = KeyEvent.KEYCODE_9;

    private VoiceCommandCenter() {}


    public static void init(@NonNull Activity activity) {
        try {
            VuzixSpeechClient client = new VuzixSpeechClient(activity);


            register(client, "Next",         KEY_NEXT);
            register(client, "Back",         KEY_BACK);
            register(client, "Scroll Up",    KEY_SCROLL_UP);
            register(client, "Scroll Down",  KEY_SCROLL_DOWN);
            register(client, "Select",       KEY_SELECT);

            register(client, "Inbound",      KEY_INBOUND);
            register(client, "Outbound",     KEY_OUTBOUND);
            register(client, "Inventory",    KEY_INVENTORY);

            register(client, "Individual",   KEY_INDIVIDUAL);
            register(client, "Consolidated", KEY_CONSOLIDATED);

            register(client, "Logout",       KEY_LOGOUT);


            register(client, "Scan",         KEY_SCAN);
            register(client, "Short",        KEY_SHORT);
            register(client, "Jump To",      KEY_JUMP_TO);
            register(client, "Remove",       KEY_SHORT_REMOVE);
            register(client, "Add",          KEY_SHORT_ADD);
            register(client, "Short Back",   KEY_SHORT_BACK);
            register(client, "Short Next",   KEY_SHORT_NEXT);


            register(client, "Zero",         KEY_DIGIT_0);
            register(client, "One",          KEY_DIGIT_1);
            register(client, "Two",          KEY_DIGIT_2);
            register(client, "Three",        KEY_DIGIT_3);
            register(client, "Four",         KEY_DIGIT_4);
            register(client, "Five",         KEY_DIGIT_5);
            register(client, "Six",          KEY_DIGIT_6);
            register(client, "Seven",        KEY_DIGIT_7);
            register(client, "Eight",        KEY_DIGIT_8);
            register(client, "Nine",         KEY_DIGIT_9);

            Log.d(TAG, "Voice phrases registered (init).");
        } catch (Exception e) {
            Log.e(TAG, "Failed to (re)register Vuzix phrases: " + e.getMessage());
        }
    }

    private static void register(VuzixSpeechClient client, String phrase, int key) {
        try {
            client.insertKeycodePhrase(phrase, key);
        } catch (Exception e) {

            Log.w(TAG, "Register phrase '" + phrase + "' warning: " + e.getMessage());
        }
    }


    public interface Actions {

        void onNext();
        void onBack();
        void onScrollUp();
        void onScrollDown();
        void onSelect();

        void onInbound();
        void onOutbound();
        void onInventory();
        void onIndividual();
        void onConsolidated();

        void onLogout();


        default void onScan() {}
        default void onShort() {}
        default void onJumpTo() {}
        default void onShortRemove() {}
        default void onShortAdd() {}
        default void onShortBack() {}
        default void onShortNext() {}
        default void onDigit(int d) {}
    }


    public static boolean handleKeyDown(int keyCode, @NonNull Actions a) {
        switch (keyCode) {

            case KEY_NEXT:         a.onNext();         return true;
            case KEY_BACK:         a.onBack();         return true;
            case KEY_SCROLL_UP:    a.onScrollUp();     return true;
            case KEY_SCROLL_DOWN:  a.onScrollDown();   return true;
            case KEY_SELECT:       a.onSelect();       return true;

            case KEY_INBOUND:      a.onInbound();      return true;
            case KEY_OUTBOUND:     a.onOutbound();     return true;
            case KEY_INVENTORY:    a.onInventory();    return true;

            case KEY_INDIVIDUAL:   a.onIndividual();   return true;
            case KEY_CONSOLIDATED: a.onConsolidated(); return true;

            case KEY_LOGOUT:       a.onLogout();       return true;


            case KEY_SCAN:         a.onScan();         return true;
            case KEY_SHORT:        a.onShort();        return true;
            case KEY_JUMP_TO:      a.onJumpTo();       return true;
            case KEY_SHORT_REMOVE: a.onShortRemove();  return true;
            case KEY_SHORT_ADD:    a.onShortAdd();     return true;
            case KEY_SHORT_BACK:   a.onShortBack();    return true;
            case KEY_SHORT_NEXT:   a.onShortNext();    return true;


            case KEY_DIGIT_0:      a.onDigit(0);       return true;
            case KEY_DIGIT_1:      a.onDigit(1);       return true;
            case KEY_DIGIT_2:      a.onDigit(2);       return true;
            case KEY_DIGIT_3:      a.onDigit(3);       return true;
            case KEY_DIGIT_4:      a.onDigit(4);       return true;
            case KEY_DIGIT_5:      a.onDigit(5);       return true;
            case KEY_DIGIT_6:      a.onDigit(6);       return true;
            case KEY_DIGIT_7:      a.onDigit(7);       return true;
            case KEY_DIGIT_8:      a.onDigit(8);       return true;
            case KEY_DIGIT_9:      a.onDigit(9);       return true;
        }
        return false;
    }
}
