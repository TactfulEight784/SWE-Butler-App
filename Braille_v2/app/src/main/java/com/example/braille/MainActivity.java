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
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
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


import android.os.Handler;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends Activity {
    private DatabaseHelper dbHelper;

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private CommandProcessor commandProcessor;
    private boolean isListeningTimeout = false; // Control variable to track timeout
    private final long TIMEOUT_DURATION = 5000; // Timeout duration in milliseconds
    private final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private final int PERMISSIONS_REQUEST_CALL_PHONE = 2;
    private final int CALL_SCREENING_PERMISSION_REQUEST = 3;
    private final int PERMISSIONS_REQUEST_SEND_SMS = 4;

    private String keyword = "destroy";
    private boolean isListening = false;
    private Handler timeoutHandler = new Handler();
    private TelecomManager telecomManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);
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
        telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        dbHelper.getWritableDatabase("EC4A783A23191FA19A2EB69864849");
        String name = "Izaiah Fleming";
        String email = "IxF69@gmail.com";
        String phone = "1234567890";

        dbHelper.insertData(name, email, phone);
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
                // Schedule the timeout handler
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
            } else if (command.toLowerCase().contains("answer phone call")) {
                answerPhoneCall();
            }
            else if (command.toLowerCase().contains("reject call")) {
                rejectCall();
            }

            else if (command.startsWith("make a call to")) {
                String contactName = extractContactName(command);
                if (contactName != null) {
                    String phoneNumber = getPhoneNumberFromContact(contactName);
                    if (phoneNumber != null) {
                        makePhoneCall(phoneNumber);
                    } else {
                        speak("Contact not found. Please provide a valid contact name.");
                    }
                } else {
                    speak("Contact name not recognized. Please provide a valid contact name.");
                }
            }
            else if (command.startsWith("search YouTube for")) {
                String query = command.substring("search YouTube for".length()).trim();
                //searchYouTube(query);
            }
            else if (command.startsWith("send a text to")) {
                String phoneNumber = extractContactName(command);
                if (phoneNumber != null) {
                    String message = extractMessage(command);
                    if (message != null) {
                        commandProcessor.sendSms(phoneNumber, message);
                    } else {
                        speak("Please provide a valid message to send.");
                    }
                }
                else {
                    speak("Contact name not recognized. Please provide a valid contact name.");
                }
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

        private void rejectCall() {
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


        private void makePhoneCall(String phoneNumber) {
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                // Check for CALL_PHONE permission
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
                    startActivity(intent);
                } else {
                    // Request CALL_PHONE permission from the user
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CALL_PHONE}, PERMISSIONS_REQUEST_CALL_PHONE);
                    speak("Permission to make phone calls is required.");
                }
            } else {
                speak("Invalid phone number. Please provide a valid phone number.");
            }
        }


        private void repeatText(String text) {
            if (text != null && !text.isEmpty()) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }
        public void sendSms(String phoneNumber, String message) {
            // Check if the app has the SEND_SMS permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                speak("Text sent to " + phoneNumber);
            } else {
                // Request SEND_SMS permission from the user
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_REQUEST_SEND_SMS);
                // The actual sending of SMS should be handled after the permission is granted in onRequestPermissionsResult
            }
        }

        /** private void searchYouTube(String query) {
         try {
         // Create a GoogleCredential using your API key
         GoogleCredential credential = new GoogleCredential().setAccessToken(API_KEY);

         // Initialize the YouTube object
         YouTube youtube = new YouTube.Builder(credential.getTransport(), credential.getJsonFactory(), null)
         .setApplicationName("YouTubeSearchApp")
         .build();

         // Call the YouTube API to perform the search
         YouTube.Search.List search = youtube.search().list(Collections.singletonList("id,snippet"));
         search.setKey("YOUR_API_KEY"); // Note: You have already set the API key above, so this line might not be needed.
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
         }**/
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
        }
        private String extractMessage(String command) {
            // Define a regular expression pattern to match a message enclosed in double quotes
            String messagePattern = "\"([^\"]*)\"";

            Pattern pattern = Pattern.compile(messagePattern);
            Matcher matcher = pattern.matcher(command);

            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return null; // Message not found in the command
            }
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

        /** private void playYouTubeVideo(String videoId) {
         Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + videoId));
         intent.setPackage("com.google.android.youtube");
         intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

         if (intent.resolveActivity(context.getPackageManager()) != null) {
         context.startActivity(intent);
         } else {
         speak("YouTube app not found on your device.");
         }
         }**/

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
        } else if (requestCode == PERMISSIONS_REQUEST_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted. You can now make the phone call.
                commandProcessor.makePhoneCall("1234567890"); // Replace with the actual phone number you want to call.
            } else {
                // Permission denied. Handle this case (e.g., show a message to the user).
                // You may not be able to make phone calls without the permission.
                speak("Permission to make phone calls was denied.");
            }
        }else if (requestCode == PERMISSIONS_REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted. You can now send the SMS.
                // Use phoneNumberToSend and messageToSend based on your app logic.
                commandProcessor.sendSms("1234567890", "Hello, This is a test text.");
            } else {
                // Permission denied. Handle this case (e.g., show a message to the user).
                speak("Permission to send SMS was denied.");
            }
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                if (telecomManager != null && telecomManager.isInCall()) {
                    try {
                        telecomManager.endCall();
                        speak("Call ended.");
                    } catch (SecurityException e) {
                        // Handle SecurityException (permission denied) appropriately
                        speak("Permission to end phone calls was denied.");
                    }
                } else {
                    speak("No active call to end.");
                }
            } else {
                // You don't have the necessary permission, request it from the user
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ANSWER_PHONE_CALLS}, PERMISSIONS_REQUEST_CALL_PHONE);
            }
            return true; // Consume the event to prevent the system's volume control behavior
        }
        return super.onKeyDown(keyCode, event);
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