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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




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
    private final int PERMISSIONS_REQUEST_RECEIVE_SMS = 5;
    private final int PERMISSIONS_REQUEST_READ_SMS = 6;

    private String keyword = "destroy";
    private boolean isListening = false;
    private Handler timeoutHandler = new Handler();
    private TelecomManager telecomManager;
    private boolean isVolumeUpPressed = false;
    private boolean isVolumeDownPressed = false;
    private boolean systemLanguageIsSpanish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {    //when the app is loaded it runs all of this
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbHelper = new DatabaseHelper(this);
        initializeTextToSpeech();
        initializeSpeechRecognizer();
        Button b = findViewById(R.id.Braille);  //sets the button to actually start the listening
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {    //on button click it stats listening
                if (isListening) {
                    stopListening();
                } else {
                    startListening();
                }
            }
        });
        String systemlanguage = Locale.getDefault().getLanguage();  //detects system language
        systemLanguageIsSpanish = systemlanguage.equals("es");

        commandProcessor = new CommandProcessor(textToSpeech, this,systemLanguageIsSpanish);    //makes processor object
        checkAndRequestCallScreeningPermission();   //checking phone call permissions
        //checks SMS Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, PERMISSIONS_REQUEST_RECEIVE_SMS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PERMISSIONS_REQUEST_READ_SMS);
        }
        telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        dbHelper.getWritableDatabase("EC4A783A23191FA19A2EB69864849");
        String name = "Izaiah Fleming";
        String email = "IxF69@gmail.com";
        String phone = "1234567890";

        dbHelper.insertData(name, email, phone);
    }
    private void checkAndRequestCallScreeningPermission() { // Checks for call screening requests
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                // Request the ANSWER_PHONE_CALLS permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ANSWER_PHONE_CALLS}, CALL_SCREENING_PERMISSION_REQUEST);
            }
        }
    }

    private void initializeTextToSpeech() { // Initializes Text to Speech
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    if(systemLanguageIsSpanish){
                        textToSpeech.setLanguage(new Locale("es","ES"));
                    }else {
                        textToSpeech.setLanguage(Locale.US);
                    }
                } else {
                    Log.e("TextToSpeech", "Initialization failed");
                }
            }
        });
    }

    private void speak(String text) {   // Text to Speech output function
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void initializeSpeechRecognizer() { // Initialize input speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        if (speechRecognizer != null) {
            speechRecognizer.setRecognitionListener(new MyRecognitionListener());
        }
    }

    private void startListening() { // Function to actually start listening
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            if (speechRecognizer != null) {
                speechRecognizer.startListening(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH));
                isListening = true;
                isListeningTimeout = false; // Reset timeout flag when starting to listen
                timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_DURATION);  // Schedule the timeout handler

            }
        }
    }

    private void stopListening() {  // Function to stop listening
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }

    private Runnable timeoutRunnable = new Runnable() { // Function to restart listener after a timeout
        @Override
        public void run() {
            isListeningTimeout = true;
            // If no speech is detected and it times out, restart listening
            startListening();
        }
    };

    private class MyRecognitionListener implements android.speech.RecognitionListener { // Basic speech recognition class that is not used
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
        public void onResults(Bundle results) { //gets the result from the listener to process it
            ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (result != null && result.size() > 0) {
                String spokenText = result.get(0);
                commandProcessor.processCommand(spokenText);    // Process the spoken text (e.g., by sending it to your command processor)
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

    public class CommandProcessor { //actually class that process the commands
        private TextToSpeech textToSpeech;
        private Context context;
        private TelecomManager telecomManager;
        private boolean isSpanish;  //true if default language is spanish

        public CommandProcessor(TextToSpeech textToSpeech, Context context, boolean isSpanish ) {
            this.textToSpeech = textToSpeech;
            this.context = context;
            this.telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            this.isSpanish = isSpanish;

        }
        public void processCommand(String command){ // chooses which language commands
            if(isSpanish){
                processSpanishCommand(command);
            }else{
                processEnglishCommand(command);
            }
        }

        private void processSpanishCommand(String command) {    //Spanish commands
            if (command.contains("prueba")) {
                String textToRepeat = command;
                repeatText(textToRepeat);
            } else if (command.toLowerCase().contains("contestar")) {
                answerPhoneCall();
            } else if (command.toLowerCase().contains("rechazar")) {
                rejectCall();
            } else if (command.contains("Llamar")) {
                String contactName = extractContactName(command);
                if (contactName != null) {
                    String phoneNumber = getPhoneNumberFromContact(contactName);
                    if (phoneNumber != null) {
                        makePhoneCall(phoneNumber);
                    } else {
                        speak("No se encontro un contacto con ese nombre. Favor de decir un nombre de contacto válido.");
                    }
                } else {
                    speak("El nombre de contacto no es reconocido. Por favor diga un nombre válido.");
                }
            } else if (command.contains("Enviar mensaje")) {
                String recipientName = extractContactName(command);
                if (recipientName != null) {
                    String phoneNumber = getPhoneNumberFromContact(recipientName);
                    if (phoneNumber != null) {
                        String message = extractMessage(command);
                        if (message != null) {
                            sendSms(phoneNumber, message);
                        } else {
                            speak("No se escucho el mensaje. Por favor, diga un mensaje para enviar.");
                        }
                    } else {
                        speak("No se encontro un contacto con ese nombre. Favor de decir un nombre de contacto válido.");
                    }
                } else {
                    speak("El nombre de contacto no es reconocido. Por favor diga un nombre válido.");
                }
            } else if (command.toLowerCase().contains("qué hora es")) {
                // Command to read the current time
                readCurrentTime();
            }else if(command.toLowerCase().contains("qué fecha es")){
                tellDate();
            }
        }

        private void processEnglishCommand(String command) {    //English commands
            if (command.contains("test")) {
                String textToRepeat = command;
                repeatText(textToRepeat);
            } else if (command.toLowerCase().contains("answer")) {
                answerPhoneCall();
            } else if (command.toLowerCase().contains("reject")) {
                rejectCall();
            } else if (command.contains("Call to")) {
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
            } else if (command.contains("send text to")) {
                String recipientName = extractContactName(command);
                if (recipientName != null) {
                    String phoneNumber = getPhoneNumberFromContact(recipientName);
                    if (phoneNumber != null) {
                        String message = extractMessage(command);
                        if (message != null) {
                            sendSms(phoneNumber, message);
                        } else {
                            speak("Message not found in the command. Please provide a message to send.");
                        }
                    } else {
                        speak("Contact not found. Please provide a valid contact name.");
                    }
                } else {
                    speak("Contact name not recognized. Please provide a valid contact name.");
                }
            } else if (command.toLowerCase().contains("what time is it")) {
                // Command to read the current time
                readCurrentTime();
            }else if(command.toLowerCase().contains("what day is it")){
                tellDate();
            }
        }
        private void tellDate() {   //command to say Date
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
            String formattedDate = dateFormat.format(currentDate);

            if(isSpanish){
                speak("La fecha es " + formattedDate);

            }else{
                speak("The current date is " + formattedDate);
            }
        }
        private void answerPhoneCall() {    //Command to answer phone call
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //has to check if it has permissions
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    if (telecomManager != null) {
                        telecomManager.acceptRingingCall();
                        if(isSpanish){
                            speak("Llamada contestada.");
                        }
                        else {
                            speak("Call answered.");
                        }
                    }
                } else {
                    if(isSpanish){
                        speak("No se otorga permiso para contestar llamadas telefónicas.");
                    }
                    else {
                        speak("Permission to answer phone calls is not granted.");
                    }
                }
            } else {
                if(isSpanish){
                    speak("No se admite contestar llamadas telefónicas en este dispositivo.");
                }
                else {
                    speak("Answering phone calls is not supported on this device.");
                }
            }
        }

        private void rejectCall() { //Function to reject call
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //has to check if it has permissions
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    if (telecomManager != null) {
                        telecomManager.endCall();
                        if(isSpanish){
                            speak("Llamada finalizada o rechazada.");
                        }
                        else {
                            speak("Call ended or rejected.");
                        }
                    }
                } else {
                    if(isSpanish){
                        speak("No se otorga permiso para finalizar llamadas telefónicas.");
                    }
                    else {
                        speak("Permission to end phone calls is not granted.");
                    }
                }
            } else {
                if(isSpanish){
                    speak("No se admite finalizar llamadas telefónicas en este dispositivo.");
                }
                else {
                    speak("Ending phone calls is not supported on this device.");
                }
            }
        }

        private void makePhoneCall(String phoneNumber) {    //Function to make phone call
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                // Check for CALL_PHONE permission
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
                    startActivity(intent);
                } else {
                    // Request CALL_PHONE permission from the user
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CALL_PHONE}, PERMISSIONS_REQUEST_CALL_PHONE);
                    if(isSpanish){
                        speak("Se requiere permiso para realizar llamadas telefónicas.");
                    }
                    else {
                        speak("Permission to make phone calls is required.");
                    }
                }
            } else {
                if(isSpanish){
                    speak("Número de teléfono no es válido. Por favor, diga un número de teléfono válido.");
                }
                else {
                    speak("Invalid phone number. Please provide a valid phone number.");
                }
            }
        }


        private void repeatText(String text) {  //used to repeat the text back
            if (text != null && !text.isEmpty()) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }
        private void sendSms(String phoneNumber, String message) {  //used to send SMS
            //has to check if it has permissions
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                if(isSpanish){
                    speak("Mensaje de texto enviado a " + phoneNumber + ": " + message);
                }
                else {
                    speak("SMS sent to " + phoneNumber + ": " + message);
                }
            } else {
                // Request SEND_SMS permission from the user
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_REQUEST_SEND_SMS);
                if(isSpanish){
                    speak("Se requiere permiso para enviar texto.");
                }
                else {
                    speak("Permission to send SMS is required.");
                }
            }
        }


        public void readCurrentTime() { //reads current time
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            if(isSpanish){

                String amOrPm;
                if (hour < 12) {
                    amOrPm = "AM";
                } else {
                    amOrPm = "PM";
                    if (hour > 12) {
                        hour -= 12;
                    }
                }

                String timeToSpeak = String.format(Locale.US, "La hora actual es %02d:%02d %s", hour, minute, amOrPm);

                speak(timeToSpeak);

            }else {

                String amOrPm;
                if (hour < 12) {
                    amOrPm = "AM";
                } else {
                    amOrPm = "PM";
                    if (hour > 12) {
                        hour -= 12;
                    }
                }

                String timeToSpeak = String.format(Locale.US, "The current time is %02d:%02d %s", hour, minute, amOrPm);

                speak(timeToSpeak);
            }
        }

        private String extractContactName(String command) { //extracts contact name to be used for calls and messages
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
        private String extractMessage(String command) { // Extracts the message from the SMS command to send the right thing
            if (command.toLowerCase().contains("mensaje")) {
                int messageIndex = command.toLowerCase().indexOf("mensaje");
                if (messageIndex != -1) {
                    return command.substring(messageIndex + "mensaje".length()).trim();
                }


                // Check if the command contains the word "message"
            }else if (command.toLowerCase().contains("message")) {
                // If "message" is found, extract the text after it
                int messageIndex = command.toLowerCase().indexOf("message");
                if (messageIndex != -1) {
                    return command.substring(messageIndex + "message".length()).trim();
                }
            }

            // If the message is not found, return null
            return null;
        }

        @SuppressLint("Range")
        private String getPhoneNumberFromContact(String contactName) {  // gets the phone number from the contract name
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

    }

    @Override
    // this is all code to check and get permissions
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CALL_SCREENING_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                if(systemLanguageIsSpanish){
                    speak("Se denegó el permiso para contestar llamadas telefónicas");
                }
                else {
                    speak("Permission to answer phone calls was denied.");
                }
            }
        } else if (requestCode == PERMISSIONS_REQUEST_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted. You can now make the phone call.
            }else{
                if(systemLanguageIsSpanish){
                    speak("Se denegó el permiso para realizar llamadas telefónicas");
                }
                else {
                    speak("Permission to make phone calls was denied.");
                }
            }
        }else if (requestCode == PERMISSIONS_REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted. You can now send SMS.
            } else {
                if(systemLanguageIsSpanish){
                    speak("Se denegó el permiso para enviar SMS");
                }else {
                    speak("Permission to send SMS was denied.");
                }
            }
        }else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { //check volume buttons for the end call command
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            isVolumeUpPressed = true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            isVolumeDownPressed = true;
        }

        if (isVolumeUpPressed && isVolumeDownPressed) {
            // Both volume buttons are pressed simultaneously, end the call
            commandProcessor.rejectCall();
            return true; // Consumes the key event
        }

        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {   // checks volume buttons for a different combination
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            isVolumeUpPressed = false;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            isVolumeDownPressed = false;
        }
        return super.onKeyUp(keyCode, event);
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
