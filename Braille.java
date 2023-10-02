import android.content.Context;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class VoiceRecognition {
    private Context context;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;

    public VoiceRecognition(Context context) {
        this.context = context;
        initializeSpeechRecognizer();
        initializeTextToSpeech();
    }
// Dylan, from here down be sure to update
    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                // Called when the speech recognition service is ready to listen
            }

            @Override
            public void onBeginningOfSpeech() {
                // Called when the user starts speaking
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Called when the RMS (Root Mean Square) changes during speech
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // Called when partial recognition results are available
            }

            @Override
            public void onEndOfSpeech() {
                // Called when the user stops speaking
            }

            @Override
            public void onError(int error) {
                // Called when an error occurs during recognition
            }

            @Override
            public void onResults(Bundle results) {
                // Called when recognition results are available
                ArrayList<String> voiceResults = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (voiceResults != null && voiceResults.size() > 0) {
                    String command = voiceResults.get(0);
                    talk("You said: " + command);
                    processCommand(command);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // Called when partial recognition results are available
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Called when an event related to speech recognition occurs
            }
        });
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            } else {
                Log.e("TextToSpeech", "Initialization failed");
            }
        });
    }

    public void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e("SpeechRecognizer", "Recognition failed: " + e.getMessage());
        }
    }

    public void talk(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void processCommand(String command) {
        // Implement your command processing logic here
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}