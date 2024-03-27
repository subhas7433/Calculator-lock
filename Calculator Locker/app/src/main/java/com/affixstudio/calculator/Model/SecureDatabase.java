package com.affixstudio.calculator.Model;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecureDatabase extends SQLiteOpenHelper {
    Context con;
    String databaseName;
    String query;
    String databasePath;
    String passphrase;



    public SecureDatabase(@Nullable Context context, @Nullable String name, String query, int version, String path)
    {
        super(context, name, null, version);
        this.con = context;
        this.databaseName = name;
        this.query = query;
        this.databasePath = path;
        this.passphrase = generatePassphrase();
        SQLiteDatabase.loadLibs(context);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        try {
            sqLiteDatabase.execSQL(query);
        } catch (Exception e) {
            Log.e("Table Failed", "" + e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // Your upgrade logic here
    }

    public Cursor getInfo() {
        Cursor cursor = this.getReadableDatabase(passphrase.toCharArray()).rawQuery("Select * from " + databaseName, null);
        return cursor;
    }

    public Cursor getData(String sql) {
        SQLiteDatabase db = this.getReadableDatabase(passphrase.toCharArray());
        return db.rawQuery(sql, null);
    }


    public SQLiteDatabase getWritableDatabase() {
        return SQLiteDatabase.openDatabase(getDatabasePath().getPath(), passphrase, null, SQLiteDatabase.OPEN_READWRITE);
    }


    public SQLiteDatabase getReadableDatabase() {
        return SQLiteDatabase.openDatabase(getDatabasePath().getPath(), passphrase, null, SQLiteDatabase.OPEN_READONLY);
    }


    public File getDatabasePath() {
        return new File(databasePath);
    }

    public String generatePassphrase() {
        try {
            String input="lsjfljoowncoisanalsieufices";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("Could not find SHA-256 algorithm", e);
        }
    }
}
