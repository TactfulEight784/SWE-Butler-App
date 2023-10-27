
package com.example.braille;

import static com.example.braille.DatabaseHelper.FeedEntry.TABLE_NAME;

import android.content.ContentValues;
import android.content.Context;
import android.util.Base64;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "BrailleContactDB";
    private static final int DATABASE_VERSION = 1;
    private static final String SECRET_KEY = "EC4A783A23191FA19A2EB69864849";

    public static class FeedEntry {
        public static final String TABLE_NAME = "UserInfo";
        public static final String _ID = "_id";
        public static final String COLUMN_NAME_TITLE = "Name";
        public static final String COLUMN_EMAIL = "Email";
        public static final String COLUMN_PHONE = "Phone";
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        SQLiteDatabase.loadLibs(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the table with encrypted columns
        String createUserTableQuery =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        FeedEntry._ID + " INTEGER PRIMARY KEY," +
                        FeedEntry.COLUMN_NAME_TITLE + " TEXT," +
                        FeedEntry.COLUMN_EMAIL + " TEXT," +
                        FeedEntry.COLUMN_PHONE + " TEXT)";

        db.execSQL(createUserTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
    public long insertData(String name, String email, String phone) {
        SQLiteDatabase db = this.getWritableDatabase(SECRET_KEY);

        ContentValues values = new ContentValues();
        values.put(FeedEntry.COLUMN_NAME_TITLE, name);
        values.put(FeedEntry.COLUMN_EMAIL, email);
        values.put(FeedEntry.COLUMN_PHONE, encryptPhone(phone)); // Encrypt the phone number before storing it

        // Insert the new row, returning the primary key value of the new row
        return db.insert(TABLE_NAME, null, values);
    }
    // Encrypt phone number using AES with CBC mode and PKCS7Padding
    private String encryptPhone(String phone) {
        try {
            SecretKey key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV(); // Get the IV (Initialization Vector)
            byte[] encryptedData = cipher.doFinal(phone.getBytes());
            byte[] combinedData = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, combinedData, 0, iv.length);
            System.arraycopy(encryptedData, 0, combinedData, iv.length, encryptedData.length);
            return Base64.encodeToString(combinedData, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Handle encryption failure
        }
    }

    // Decrypt phone number using AES with CBC mode and PKCS7Padding
    private String decryptPhone(String encryptedPhone) {
        try {
            byte[] combinedData = Base64.decode(encryptedPhone, Base64.DEFAULT);
            byte[] iv = Arrays.copyOfRange(combinedData, 0, 16); // IV size is 16 bytes for AES
            byte[] encryptedData = Arrays.copyOfRange(combinedData, 16, combinedData.length);

            SecretKey key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] decryptedData = cipher.doFinal(encryptedData);
            return new String(decryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Handle decryption failure
        }
    }

}
