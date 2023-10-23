package com.example.braille;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.SearchResultSnippet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private CommandProcessor commandProcessor;
    private boolean isListeningTimeout = false; // Control variable to track timeout
    private final long TIMEOUT_DURATION = 5000; // Timeout duration in milliseconds
    private final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private String keyword = "destroy";
    private boolean isListening = false;
    private Handler timeoutHandler = new Handler();
    private final int CALL_SCREENING_PERMISSION_REQUEST = 2;
    private static final String API_KEY = "YOUR_YOUTUBE_API_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Replace with your layout file

        initializeTextToSpeech();
        initializeSpeechRecognizer();
        Button b = findViewById(R.id.Braille);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isListening) {
                    stopListening();
                } else {
                    startListening();
                }
            }
        });
        commandProcessor = new CommandProcessor(textToSpeech, this);
        checkAndRequestCallScreeningPermission();
    }

    private void checkAndRequestCallScreeningPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                // Request the ANSWER_PHONE_CALLS permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ANSWER_PHONE_CALLS}, CALL_SCREENING_PERMISSION_REQUEST);
            }
        }
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.US);
                } else {
                    Log.e("TextToSpeech", "Initialization failed");
                }
            }
        });
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        if (speechRecognizer != null) {
            speechRecognizer.setRecognitionListener(new MyRecognitionListener());
        }
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            if (speechRecognizer != null) {
                speechRecognizer.startListening(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH));
                isListening = true;
                isListeningTimeout = false; // Reset timeout flag when starting to listen
                // Schedule a timeout handler
                timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_DURATION);
            }
        }
    }

    private void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }

    private Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            isListeningTimeout = true;
            // If no speech is detected and it times out, restart listening
            startListening();
        }
    };

    private class MyRecognitionListener implements android.speech.RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
            // Called when the recognizer is ready for speech
        }

        @Override
        public void onBeginningOfSpeech() {
            // Called when the user has started to speak
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            // Called when the RMS value of the audio being processed changes
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            // Called when partial recognition results are available
        }

        @Override
        public void onEndOfSpeech() {
            // Called when the user has finished speaking
        }

        @Override
        public void onError(int error) {
            // Called when an error occurs
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (result != null && result.size() > 0) {
                String spokenText = result.get(0);
                // Process the spoken text (e.g., by sending it to your command processor)
                commandProcessor.processCommand(spokenText);
                if (spokenText.toLowerCase().contains(keyword)) {
                    stopListening();
                } else {
                    startListening();
                }
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            // Called when partial recognition results are available
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            // Called when an event related to recognition occurs
        }
    }

    public class CommandProcessor {
        private TextToSpeech textToSpeech;
        private Context context;
        private TelecomManager telecomManager;

        public CommandProcessor(TextToSpeech textToSpeech, Context context) {
            this.textToSpeech = textToSpeech;
            this.context = context;
            this.telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);

        }

        public void processCommand(String command) {
            if (command.contains("test")) {
                String textToRepeat = command;
                repeatText(textToRepeat);
            }
            else if (command.toLowerCase().contains("answer phone call")) {
                answerPhoneCall();
            }
            else if (command.toLowerCase().contains("reject call")) {
                RejectCall();
            }
            else if (command.contains("make a call to")) {
                String contactName = extractContactName(command);
                String phoneNumber = getPhoneNumberFromContact(contactName);
            }
            else if (command.startsWith("search YouTube for")) {
                String query = command.substring("search YouTube for".length()).trim();
                searchYouTube(query);
            }
            else {
                speak("Command not recognized");
            }
        }

        private void answerPhoneCall() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    if (telecomManager != null) {
                        telecomManager.acceptRingingCall();
                        speak("Call answered.");
                    }
                } else {
                    speak("Permission to answer phone calls is not granted.");
                }
            } else {
                speak("Answering phone calls is not supported on this device.");
            }
        }
        private void RejectCall() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    if (telecomManager != null) {
                        telecomManager.endCall();
                        speak("Call rejected.");
                    }
                } else {
                    speak("Permission to end phone calls is not granted.");
                }
            } else {
                speak("Ending phone calls is not supported on this device.");
            }
        }
        private String extractContactName(String command) {
            // Define a regular expression pattern to match names (assuming first and last name)
            String namePattern = "([A-Z][a-z]+)\\s+([A-Z][a-z]+)";
            Pattern pattern = Pattern.compile(namePattern);
            Matcher matcher = pattern.matcher(command);

            if (matcher.find()) {
                // If a name is found return it
                return matcher.group(0);
            } else {
                // No name found in the command
                return null;
            }
            // Replace with actual logic
        }

        @SuppressLint("Range")
        private String getPhoneNumberFromContact(String contactName) {
            ContentResolver contentResolver = getContentResolver();
            String phoneNumber = null;

            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
            String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " = ?";
            String[] selectionArgs = {contactName};

            Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, null);

            if (cursor != null && cursor.moveToFirst()) {
                phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                cursor.close();
            }

            return phoneNumber;
        }

        private void makePhoneCall(String phoneNumber) {
            String uri = "tel:" + phoneNumber;
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(uri));
            startActivity(intent);
        }

        private void repeatText(String text) {
            if (text != null && !text.isEmpty()) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }
        private void searchYouTube(String query) {
            try {
                // Create a GoogleCredential using your API key
                GoogleCredential credential = new GoogleCredential().setAccessToken(API_KEY);

                // Initialize the YouTube object
                YouTube youtube = new YouTube.Builder(credential.getTransport(), credential.getJsonFactory(), null)
                        .setApplicationName("YouTubeSearchApp")
                        .build();

                // Call the YouTube API to perform the search
                YouTube.Search.List search = youtube.search().list(Collections.singletonList("id,snippet"));
                search.setKey("YOUR_API_KEY");
                search.setQ(query);
                search.setType(Collections.singletonList("video"));

                SearchListResponse searchResponse = search.execute();
                List<SearchResult> searchResults = searchResponse.getItems();

                if (searchResults != null && !searchResults.isEmpty()) {
                    SearchResult firstResult = searchResults.get(0);
                    SearchResultSnippet snippet = firstResult.getSnippet();
                    String videoId = firstResult.getId().getVideoId();
                    String title = snippet.getTitle();
                    String description = snippet.getDescription();

                    // Play the first search result
                    playYouTubeVideo(videoId);
                    speak("Playing video: " + title + ". Description: " + description);
                } else {
                    speak("No matching videos found on YouTube.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void playYouTubeVideo(String videoId) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + videoId));
            intent.setPackage("com.google.android.youtube");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                speak("YouTube app not found on your device.");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CALL_SCREENING_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted. Implement your call answering logic here.
                commandProcessor.answerPhoneCall();
            } else {
                // Permission denied. Handle this case (e.g., show a message to the user).
                // You may not be able to answer phone calls without the permission.
                speak("Permission to answer phone calls was denied.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        super.onDestroy();
    }
}