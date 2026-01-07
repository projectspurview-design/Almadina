package com.example.Pickbyvision.voice;

import android.app.Activity;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import com.example.Pickbyvision.Consolidated_Pick.CompletedordersummaryconsolidatedActivity;
import com.example.Pickbyvision.Consolidated_Pick.NextorderconsolidatedActivity;
import com.example.Pickbyvision.Consolidated_Pick.OrdecompleteconsolidatedActivity;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

/** Centralized voice setup + routing for every screen. Safe to call on every resume. */
public final class VoiceCommandCenter {

    private static final String TAG = "VoiceCommandCenter";

    // ====== Unified keys kept for existing screens (Sixth/Seventh/Eighth/Ninth) ======

    // ====== OrdecompleteconsolidatedActivity profile (match existing behavior) ======
    public static final int OCC_KEY_NEXT   = KeyEvent.KEYCODE_F2;
    public static final int OCC_KEY_LOGOUT = KeyEvent.KEYCODE_F8;
    public static final int NOC_KEY_NEXT = KeyEvent.KEYCODE_F2; // keep EXACT behavior from NextorderconsolidatedActivity
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

    // ====== CompletedordersummaryconsolidatedActivity profile (match existing behavior) ======
    public static final int COS_KEY_NEXT_SUMMARY = KeyEvent.KEYCODE_F9; // your old KEYCODE_NEXT_SUMMARY
    public static final int COS_KEY_VOICE_NEXT   = KeyEvent.KEYCODE_F7; // your old KEYCODE_VOICE_NEXT (handled too)

    // ====== ConsolidatedJobActivity profile (uses the SAME keys your activity already used) ======
    public static final int CJ_KEY_SCROLL_DOWN   = KeyEvent.KEYCODE_F1;
    public static final int CJ_KEY_STOP_SCROLL   = KeyEvent.KEYCODE_F2;
    public static final int CJ_KEY_SCAN          = KeyEvent.KEYCODE_F3;
    public static final int CJ_KEY_SHORT         = KeyEvent.KEYCODE_F4;

    public static final int CJ_KEY_SHORT_REMOVE  = KeyEvent.KEYCODE_F5;
    public static final int CJ_KEY_SHORT_ADD     = KeyEvent.KEYCODE_F6;
    public static final int CJ_KEY_SHORT_BACK    = KeyEvent.KEYCODE_F7;
    public static final int CJ_KEY_SHORT_NEXT    = KeyEvent.KEYCODE_F8;

    public static final int CJ_KEY_LOGOUT        = KeyEvent.KEYCODE_F10;
    public static final int CJ_KEY_JUMP_TO       = KeyEvent.KEYCODE_F11;

    // "Next Order" / "Go to Next"
    public static final int CJ_KEY_NEXT_ORDER    = KeyEvent.KEYCODE_FORWARD;

    // ====== Extra keys for TenthActivity features (no conflicts with above) ======
    public static final int KEY_SCAN         = KeyEvent.KEYCODE_BUTTON_7;  // safe          // "Scan"
    public static final int KEY_SHORT         = KeyEvent.KEYCODE_BUTTON_1;      // "Short" (open short qty UI)
    public static final int KEY_JUMP_TO       = KeyEvent.KEYCODE_BUTTON_2;      // "Jump To"
    public static final int KEY_SHORT_REMOVE  = KeyEvent.KEYCODE_BUTTON_3;      // "Remove" (short qty backspace)
    public static final int KEY_SHORT_ADD     = KeyEvent.KEYCODE_BUTTON_4;      // "Add" (short qty append digit)
    public static final int KEY_SHORT_BACK    = KeyEvent.KEYCODE_BUTTON_5;      // "Short Back" (cancel short)
    public static final int KEY_SHORT_NEXT    = KeyEvent.KEYCODE_BUTTON_6;      // "Short Next" (confirm short)

    public static final int KEY_UP_STEP     = 1000;  // "Up"
    public static final int KEY_DOWN_STEP   = 1001;  // "Down"
    public static final int KEY_GO_UP       = 1002;  // "Go Up"
    public static final int KEY_GO_DOWN     = 1003;  // "Go Down"
    public static final int KEY_STOP        = 1004;
    // Digits for short-quantity via voice
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

    /** Call this from onResume() (and onCreate() is fine too). Idempotent. */
    public static void init(@NonNull Activity activity) {
        try {
            VuzixSpeechClient client = new VuzixSpeechClient(activity);

            // ====== Core navigation (existing screens) ======
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

            // ====== TenthActivity phrases (added) ======
            register(client, "Scan",         KEY_SCAN);
            register(client, "Short",        KEY_SHORT);
            register(client, "Jump To",      KEY_JUMP_TO);
            register(client, "Remove",       KEY_SHORT_REMOVE);
            register(client, "Add",          KEY_SHORT_ADD);
            register(client, "Short Back",   KEY_SHORT_BACK);
            register(client, "Short Next",   KEY_SHORT_NEXT);

            // Digits (for short quantity input)
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

            register(client, "Up",       KEY_UP_STEP);
            register(client, "Down",     KEY_DOWN_STEP);
            register(client, "Go Up",    KEY_GO_UP);
            register(client, "Go Down",  KEY_GO_DOWN);
            register(client, "Stop",     KEY_STOP);

            Log.d(TAG, "Voice phrases registered (init).");
        } catch (Exception e) {
            Log.e(TAG, "Failed to (re)register Vuzix phrases: " + e.getMessage());
        }
    }

    private static void register(VuzixSpeechClient client, String phrase, int key) {
        try {
            client.insertKeycodePhrase(phrase, key);
        } catch (Exception e) {
            // Duplicate or SDK warnings are OK to ignore
            Log.w(TAG, "Register phrase '" + phrase + "' warning: " + e.getMessage());
        }
    }

    public static void initConsolidatedJob(@NonNull Activity activity) {
        try {
            VuzixSpeechClient client = new VuzixSpeechClient(activity);

            safeDelete(client, "BACK");
            safeDelete(client, "GO BACK");
            safeDelete(client, "Go Back");

            // also ensure "Next" is mapped exactly for this screen
            safeDelete(client, "Next");

            // --- cleanup (copied from your activity) ---
            safeDelete(client, "Next");
            safeDelete(client, "Next Order");
            safeDelete(client, "Go to Next");
            safeDelete(client, "Short");
            safeDelete(client, "BACK");
            safeDelete(client, "GO BACK");

            safeDelete(client, "Jump To");
            safeDelete(client, "Scan Barcode");
            safeDelete(client, "Scan");
            safeDelete(client, "Add");
            safeDelete(client, "Remove");
            safeDelete(client, "Back");
            safeDelete(client, "OK");
            safeDelete(client, "Ok");
            safeDelete(client, "Okay");
            safeDelete(client, "CLOSE");
            safeDelete(client, "Close");
            safeDelete(client, "LOG OUT");
            safeDelete(client, "LOGOUT");

            safeDelete(client, "BACK");
            safeDelete(client, "GO BACK");
            safeDelete(client, "Go Back");

            // Remove any conflicting mappings from your global init (important because F2 is Scroll Up globally)
            safeDelete(client, "Next");
            safeDelete(client, "Scroll Up");

            safeDelete(client, "Logout");
            safeDelete(client, "Log Out");

            // Register EXACT behavior from OrdecompleteconsolidatedActivity
            register(client, "Next",   OCC_KEY_NEXT);
            register(client, "Logout", OCC_KEY_LOGOUT);
            register(client, "Log Out", OCC_KEY_LOGOUT);

            for (int i = 0; i <= 9; i++) safeDelete(client, "Number " + i);

            // --- register (copied from your activity) ---
            register(client, "Scroll Down", CJ_KEY_SCROLL_DOWN);
            register(client, "Stop",        CJ_KEY_STOP_SCROLL);
            register(client, "Next", NOC_KEY_NEXT);


            register(client, "Scan Barcode", CJ_KEY_SCAN);
            register(client, "Scan",         CJ_KEY_SCAN);

            register(client, "Short",  CJ_KEY_SHORT);
            register(client, "Jump To", CJ_KEY_JUMP_TO);

            register(client, "Add",    CJ_KEY_SHORT_ADD);
            register(client, "Remove", CJ_KEY_SHORT_REMOVE);
            register(client, "Back",   CJ_KEY_SHORT_BACK);

            register(client, "Next",       CJ_KEY_SHORT_NEXT);
            register(client, "Next Order", CJ_KEY_NEXT_ORDER);
            register(client, "Go to Next", CJ_KEY_NEXT_ORDER);

            register(client, "LOGOUT",  CJ_KEY_LOGOUT);
            register(client, "LOG OUT", CJ_KEY_LOGOUT);

            // digits as you used ("0".."9")
            register(client, "0", KeyEvent.KEYCODE_0);
            register(client, "1", KeyEvent.KEYCODE_1);
            register(client, "2", KeyEvent.KEYCODE_2);
            register(client, "3", KeyEvent.KEYCODE_3);
            register(client, "4", KeyEvent.KEYCODE_4);
            register(client, "5", KeyEvent.KEYCODE_5);
            register(client, "6", KeyEvent.KEYCODE_6);
            register(client, "7", KeyEvent.KEYCODE_7);
            register(client, "8", KeyEvent.KEYCODE_8);
            register(client, "9", KeyEvent.KEYCODE_9);

            Log.d(TAG, "ConsolidatedJob voice phrases registered.");
        } catch (Exception e) {
            Log.e(TAG, "initConsolidatedJob failed: " + e.getMessage());
        }
    }
    public static boolean handleKeyDownConsolidatedJob(int keyCode, @NonNull Actions a) {
        switch (keyCode) {
            case CJ_KEY_SCROLL_DOWN: a.onScrollDown(); return true;
            case CJ_KEY_STOP_SCROLL: a.onStop();       return true;

            case CJ_KEY_SCAN:        a.onScan();       return true;
            case CJ_KEY_SHORT:       a.onShort();      return true;
            case CJ_KEY_JUMP_TO:     a.onJumpTo();     return true;

            case CJ_KEY_SHORT_REMOVE:a.onShortRemove();return true;
            case CJ_KEY_SHORT_ADD:   a.onShortAdd();   return true;
            case CJ_KEY_SHORT_BACK:  a.onShortBack();  return true;
            case CJ_KEY_SHORT_NEXT:  a.onShortNext();  return true;

            case CJ_KEY_LOGOUT:      a.onLogout();     return true;
            case CJ_KEY_NEXT_ORDER:  a.onNextOrder();  return true;

            // digits
            case KeyEvent.KEYCODE_0: a.onDigit(0); return true;
            case KeyEvent.KEYCODE_1: a.onDigit(1); return true;
            case KeyEvent.KEYCODE_2: a.onDigit(2); return true;
            case KeyEvent.KEYCODE_3: a.onDigit(3); return true;
            case KeyEvent.KEYCODE_4: a.onDigit(4); return true;
            case KeyEvent.KEYCODE_5: a.onDigit(5); return true;
            case KeyEvent.KEYCODE_6: a.onDigit(6); return true;
            case KeyEvent.KEYCODE_7: a.onDigit(7); return true;
            case KeyEvent.KEYCODE_8: a.onDigit(8); return true;
            case KeyEvent.KEYCODE_9: a.onDigit(9); return true;
        }
        return false;
    }

    private static void safeDelete(VuzixSpeechClient client, String phrase) {
        try { client.deletePhrase(phrase); } catch (Exception ignored) {}
    }

    public static void initOrderCompleteConsolidated(@NonNull Activity activity) {
        try {
            VuzixSpeechClient client = new VuzixSpeechClient(activity);

            // Cleanup you were doing in the activity
            safeDelete(client, "BACK");
            safeDelete(client, "GO BACK");
            safeDelete(client, "Go Back");

            // Remove any conflicting mappings from your global init (important because F2 is Scroll Up globally)
            safeDelete(client, "Next");
            safeDelete(client, "Scroll Up");

            safeDelete(client, "Logout");
            safeDelete(client, "Log Out");

            // Register EXACT behavior from OrdecompleteconsolidatedActivity
            register(client, "Next",   OCC_KEY_NEXT);
            register(client, "Logout", OCC_KEY_LOGOUT);
            register(client, "Log Out", OCC_KEY_LOGOUT);

            Log.d(TAG, "Ordecompleteconsolidated voice phrases registered.");
        } catch (Exception e) {
            Log.e(TAG, "initOrderCompleteConsolidated failed: " + e.getMessage());
        }
    }



    public static void initCompletedOrderSummaryConsolidated(@NonNull Activity activity) {
        try {
            VuzixSpeechClient client = new VuzixSpeechClient(activity);

            // Clean up any existing phrases first (copied from your activity)
            safeDelete(client, "Next");
            safeDelete(client, "Next Order");
            safeDelete(client, "Go to Next");
            safeDelete(client, "OK");
            safeDelete(client, "Ok");
            safeDelete(client, "Okay");
            safeDelete(client, "CLOSE");
            safeDelete(client, "Close");
            safeDelete(client, "GO BACK");
            safeDelete(client, "BACK");
            safeDelete(client, "Go Back");

            // IMPORTANT: global init() registers "Back" -> F9, which conflicts with this screen
            safeDelete(client, "Back");

            // Register voice commands for next button (same as before)
            register(client, "Next",      COS_KEY_NEXT_SUMMARY);
            register(client, "Next Order",COS_KEY_NEXT_SUMMARY);
            register(client, "Go to Next",COS_KEY_NEXT_SUMMARY);

            Log.i(TAG, "Summary screen voice phrases registered.");
        } catch (Exception e) {
            Log.e(TAG, "initCompletedOrderSummaryConsolidated failed: " + e.getMessage());
        }
    }
    public static boolean handleKeyDownCompletedOrderSummaryConsolidated(int keyCode, @NonNull Actions a) {
        if (keyCode == COS_KEY_NEXT_SUMMARY || keyCode == COS_KEY_VOICE_NEXT) {
            a.onNext();
            return true;
        }
        return false;
    }

    public static void initNextOrderConsolidated(@NonNull Activity activity) {
        try {
            VuzixSpeechClient client = new VuzixSpeechClient(activity);

            // Clear conflicting phrases that might have been registered by other screens
            safeDelete(client, "Next");
            safeDelete(client, "Scroll Up"); // because globally "Scroll Up" uses F2

            // If you want Back blocked on this screen, remove these (optional)
            safeDelete(client, "BACK");
            safeDelete(client, "GO BACK");
            safeDelete(client, "Go Back");

            // IMPORTANT: map Next exactly like your old activity did
            register(client, "Next", NOC_KEY_NEXT); // F2

            Log.d(TAG, "Nextorderconsolidated voice phrases registered.");
        } catch (Exception e) {
            Log.e(TAG, "initNextOrderConsolidated failed: " + e.getMessage());
        }
    }

    // ====== Actions contract ======
    public interface Actions {
        // Common screens (required)
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

        // ConsolidatedJob / other screens (optional)
        default void onNextOrder() {}

        // Up/Down/Stop group (optional)
        default void onUp() {}
        default void onDown() {}
        default void onGoUp() {}
        default void onGoDown() {}
        default void onStop() {}

        // Extra hooks (optional)
        default void onScan() {}
        default void onShort() {}
        default void onJumpTo() {}
        default void onShortRemove() {}
        default void onShortAdd() {}
        default void onShortBack() {}
        default void onShortNext() {}
        default void onDigit(int d) {}
    }


    public static boolean handleKeyDownNextOrderConsolidated(int keyCode, @NonNull Actions a) {
        if (keyCode == NOC_KEY_NEXT) {
            a.onNext();
            return true;
        }
        return false;
    }

    public static void initConsolidatedTransaction(@NonNull Activity activity) {
        try {
            VuzixSpeechClient client = new VuzixSpeechClient(activity);

            safeDelete(client, "Logout");
            safeDelete(client, "Log Out");

            register(client, "Logout", KEY_LOGOUT);
            register(client, "Log Out", KEY_LOGOUT);

            Log.d(TAG, "ConsolidatedTransaction logout voice registered");
        } catch (Exception e) {
            Log.e(TAG, "initConsolidatedTransaction failed", e);
        }
    }

    public static boolean handleKeyDownOrderCompleteConsolidated(int keyCode, @NonNull Actions a) {
        if (keyCode == OCC_KEY_NEXT) {
            a.onNext();
            return true;
        }
        if (keyCode == OCC_KEY_LOGOUT) {
            a.onLogout();
            return true;
        }
        return false;
    }
    /** Centralized key routing. Return true if handled. */
    public static boolean handleKeyDown(int keyCode, @NonNull Actions a) {
        switch (keyCode) {
            // Existing

            case KEY_UP_STEP:     a.onUp();     return true;
            case KEY_DOWN_STEP:   a.onDown();   return true;
            case KEY_GO_UP:       a.onGoUp();   return true;
            case KEY_GO_DOWN:     a.onGoDown(); return true;
            case KEY_STOP:        a.onStop();   return true;
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

            // TenthActivity extras
            case KEY_SCAN:         a.onScan();         return true;
            case KEY_SHORT:        a.onShort();        return true;
            case KEY_JUMP_TO:      a.onJumpTo();       return true;
            case KEY_SHORT_REMOVE: a.onShortRemove();  return true;
            case KEY_SHORT_ADD:    a.onShortAdd();     return true;
            case KEY_SHORT_BACK:   a.onShortBack();    return true;
            case KEY_SHORT_NEXT:   a.onShortNext();    return true;

            // Digits
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
