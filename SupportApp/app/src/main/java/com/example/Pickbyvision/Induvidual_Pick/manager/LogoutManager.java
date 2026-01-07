package com.example.Pickbyvision.Induvidual_Pick.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import com.example.Pickbyvision.Induvidual_Pick.Barcodescanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LogoutManager {
    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_NAME = "userName";
    private static final String TAG = "LogoutManager";


    private static final List<String> LOGOUT_KEYWORDS = Arrays.asList(
            "logout", "log out", "sign out", "signout", "exit", "quit"
    );


    public static void performLogout(Context context) {
        // Clear user session data
        clearUserSession(context);

        // Navigate to FourthActivity (login screen)
        Intent intent = new Intent(context, Barcodescanner.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }


    private static void clearUserSession(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Clear login status and user data
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_USER_NAME);

        // Apply changes
        editor.apply();
    }


    public static boolean isUserLoggedIn(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }


    public static void startVoiceLogoutListener(Activity activity) {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            Toast.makeText(activity, "Voice recognition not available", Toast.LENGTH_SHORT).show();
            return;
        }

        SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'logout' to sign out");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        speechRecognizer.setRecognitionListener(new VoiceLogoutListener(activity, speechRecognizer));
        speechRecognizer.startListening(recognizerIntent);

        Toast.makeText(activity, "Listening for logout command...", Toast.LENGTH_SHORT).show();
    }


    private static boolean containsLogoutKeyword(String spokenText) {
        String lowerCaseText = spokenText.toLowerCase().trim();
        for (String keyword : LOGOUT_KEYWORDS) {
            if (lowerCaseText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }


    private static class VoiceLogoutListener implements RecognitionListener {
        private final Activity activity;
        private final SpeechRecognizer speechRecognizer;

        public VoiceLogoutListener(Activity activity, SpeechRecognizer speechRecognizer) {
            this.activity = activity;
            this.speechRecognizer = speechRecognizer;
        }

        @Override
        public void onReadyForSpeech(android.os.Bundle params) {
            Log.d(TAG, "Ready for speech");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "Speech started");
        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "Speech ended");
        }

        @Override
        public void onError(int error) {
            String errorMessage = getErrorText(error);
            Log.e(TAG, "Speech recognition error: " + errorMessage);
            Toast.makeText(activity, "Voice recognition error: " + errorMessage, Toast.LENGTH_SHORT).show();
            speechRecognizer.destroy();
        }

        @Override
        public void onResults(android.os.Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String spokenText = matches.get(0);
                Log.d(TAG, "Recognized text: " + spokenText);

                if (containsLogoutKeyword(spokenText)) {
                    Toast.makeText(activity, "Voice logout command recognized", Toast.LENGTH_SHORT).show();
                    performLogout(activity);
                } else {
                    Toast.makeText(activity, "Command not recognized. Say 'logout' to sign out.", Toast.LENGTH_SHORT).show();
                }
            }
            speechRecognizer.destroy();
        }

        @Override
        public void onPartialResults(android.os.Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, android.os.Bundle params) {

        }

        private String getErrorText(int errorCode) {
            switch (errorCode) {
                case SpeechRecognizer.ERROR_AUDIO:
                    return "Audio recording error";
                case SpeechRecognizer.ERROR_CLIENT:
                    return "Client side error";
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    return "Insufficient permissions";
                case SpeechRecognizer.ERROR_NETWORK:
                    return "Network error";
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    return "Network timeout";
                case SpeechRecognizer.ERROR_NO_MATCH:
                    return "No match found";
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    return "Recognition service busy";
                case SpeechRecognizer.ERROR_SERVER:
                    return "Server error";
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    return "No speech input";
                default:
                    return "Unknown error";
            }
        }
    }
}