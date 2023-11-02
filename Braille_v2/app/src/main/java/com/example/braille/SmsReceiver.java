package com.example.braille;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsMessage;
import android.util.Log;
import java.util.Locale;

public class SmsReceiver extends BroadcastReceiver {
    private TextToSpeech textToSpeech;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            Object[] smsObj = (Object[]) bundle.get("pdus");

            for (Object obj : smsObj) {
                SmsMessage message = SmsMessage.createFromPdu((byte[]) obj);

                String mobNo = message.getDisplayOriginatingAddress();
                String msg = message.getDisplayMessageBody();

                Log.d("MsgDetails", "MobNo: " + mobNo + ", Msg: " + msg);

                readSmsWithTts(context, mobNo, msg);
            }
        }
    }

    private void readSmsWithTts(Context context, String mobNo, String msg) {
        String smsText = "You have a new message from " + mobNo + ": " + msg;

        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(context, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.US);
                    textToSpeech.speak(smsText, TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    Log.e("TextToSpeech", "Initialization failed");
                }
            });
        } else {
            textToSpeech.speak(smsText, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
}
