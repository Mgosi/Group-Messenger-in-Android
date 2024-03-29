package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

//https://developer.android.com/training/data-storage/sqlite.html
public class SQL_Helper extends SQLiteOpenHelper {
    public static final String DB_NAME = "GroupMessenger273";                                           //Name of the Database to create.
    public static final int DB_VERSION = 1;

    public SQL_Helper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);                                          //Constructor which calls SQLiteOpenHelper's constructor
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Query to create a table in the DB
        final String CREATE_TABLE_SQL = "CREATE TABLE " + FeedReaderContract.FeedEntry.TABLE_NAME +"("     +
                FeedReaderContract.FeedEntry.KEY_NAME + " PRIMARY KEY NOT NULL, " +
                FeedReaderContract.FeedEntry.VALUE_NAME + " TEXT NOT NULL)";
        System.out.println("Inserting Table");
        //Execute the query
        db.execSQL(CREATE_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Query to delete the table if a newer version is made.
        final String DELETE_TABLE_SQL = "DROP TABLE IF EXISTS " + FeedReaderContract.FeedEntry.TABLE_NAME;
        db.execSQL(DELETE_TABLE_SQL);
    }
}
