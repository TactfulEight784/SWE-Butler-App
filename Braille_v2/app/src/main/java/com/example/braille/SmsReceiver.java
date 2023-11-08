package com.example.braille;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsMessage;
import android.util.Log;
import java.util.Locale;
import android.telephony.SmsManager;
import android.database.Cursor;
import android.provider.ContactsContract;

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
        } else if (intent.getAction().equals("com.example.braille.EMAIL_NOTIFICATION")) {
            // Handle email notifications
            String sender = intent.getStringExtra("sender");
            String subject = intent.getStringExtra("subject");
            readEmailWithTts(context, sender, subject);
        }
    }

    private void readSmsWithTts(Context context, String mobNo, String msg) {
        String systemLanguage = Locale.getDefault().getLanguage();
        String senderName = getContactName(context, mobNo);
        Locale ttsLocale;
        String smsText;
        if (systemLanguage.equals("es")) {
            ttsLocale = new Locale("es", "ES"); // Spanish (Spain) locale
            if (senderName != null) {
                smsText = "Tienes un nuevo mensaje de " + senderName + ": " + msg;
            } else {
                smsText = "Tienes un nuevo mensaje de " + mobNo + ": " + msg;
            }
        } else {
            ttsLocale = Locale.US; // English (United States) locale
            if (senderName != null) {
                smsText = "You have a new message from " + senderName + ": " + msg;
            } else {
                smsText = "You have a new message from " + mobNo + ": " + msg;
            }
        }
        if (textToSpeech == null) {
            Locale finalTtsLocale = ttsLocale;
            textToSpeech = new TextToSpeech(context, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(finalTtsLocale);
                    textToSpeech.speak(smsText, TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    Log.e("TextToSpeech", "Initialization failed");
                }
            });
        } else {
            textToSpeech.setLanguage(ttsLocale);
            textToSpeech.speak(smsText, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    private void readEmailWithTts(Context context, String sender, String subject) {
        String systemLanguage = Locale.getDefault().getLanguage();
        Locale ttsLocale;
        String emailText;
        if (systemLanguage.equals("es")) {
            ttsLocale = new Locale("es", "ES"); // Spanish (Spain) locale
            emailText = "Tienes un nuevo correo de " + sender + " con el asunto: " + subject;
        } else {
            ttsLocale = Locale.US; // English (United States) locale
            emailText = "You have a new email from " + sender + " with the subject: " + subject;
        }

        if (textToSpeech == null) {
            Locale finalTtsLocale = ttsLocale;
            textToSpeech = new TextToSpeech(context, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(finalTtsLocale);
                    textToSpeech.speak(emailText, TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    Log.e("TextToSpeech", "Initialization failed");
                }
            });
        } else {
            textToSpeech.setLanguage(ttsLocale);
            textToSpeech.speak(emailText, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    private String getContactName(Context context, String phoneNumber) {
        String contactName = null;

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

        Cursor cursor = context.getContentResolver().query(contactUri, projection, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
            contactName = cursor.getString(nameIndex);
            cursor.close();
        }

        return contactName;
    }
}
