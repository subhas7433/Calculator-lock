package com.affixstudio.calculator.Model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class Database extends SQLiteOpenHelper {
    Context con;
    String databaseName;
    String query;

    public Database(@Nullable Context context, @Nullable String name, String query, int version) {
        super(context, name,null,version);
        this.con=context;
        this.databaseName=name;
        this.query=query;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {


        try
        {
            sqLiteDatabase.execSQL(query);
        }
        catch (Exception e)
        {
            Log.e("Table Failed",""+e);
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion)
    {
        if (oldVersion < 2)
        {
            // we're at version 2, so the code below will add the new column
            String alterTableQuery = "ALTER TABLE "+databaseName+" ADD COLUMN ConversationID text DEFAULT '0'";
            sqLiteDatabase.execSQL(alterTableQuery);

        }else if (oldVersion < 3)
        {
            // we're at version 3, so the code below will add the new column
            String alterTableQuery = "ALTER TABLE "+databaseName+" ADD COLUMN webAccess text DEFAULT '0' ";
            String alterTableQuery2 = "ALTER TABLE "+databaseName+" ADD COLUMN webLinks text DEFAULT '0' ";
            String alterTableQuery3 = "ALTER TABLE "+databaseName+" ADD COLUMN aiIndex text DEFAULT '0' ";
            sqLiteDatabase.execSQL(alterTableQuery);
            sqLiteDatabase.execSQL(alterTableQuery2);
            sqLiteDatabase.execSQL(alterTableQuery3);
        }
    }



    public Cursor getInfo()
    {

        Cursor cursor= this.getReadableDatabase().rawQuery("Select * from " + databaseName/*table and database name is same*/
                , null);
        return cursor;
    }

    public Cursor getData(String sql) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(sql, null);
    }




}
