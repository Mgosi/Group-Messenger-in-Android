package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import static android.database.sqlite.SQLiteDatabase.openOrCreateDatabase;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 *
 * Please read:
 *
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 *
 * before you start to get yourself familiarized with ContentProvider.
 *
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {
    SQL_Helper messengerSQLHelper;


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }
    //https://developer.android.com/guide/topics/providers/content-provider-creating.html
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */

        //https://developer.android.com/training/data-storage/sqlite.html                               //Used to create a Database using SQLite.
        SQLiteDatabase messenger_db = messengerSQLHelper.getWritableDatabase();                         //This creates a Database through which we can save the key, value paris.
        long _id = messenger_db.insert(FeedReaderContract.FeedEntry.TABLE_NAME,null, values);   //Used to insert into the the table with the content values

        Log.v("insert", values.toString());
        uri = ContentUris.withAppendedId(uri, _id);                                                 //https://developer.android.com/reference/android/content/ContentUris.html#withAppendedId(android.net.Uri,%20long) to add the new row to the uri
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        //messenger = SQLiteOpenHelper.getWritableDatabase();
        messengerSQLHelper = new SQL_Helper(getContext());                                             //Calls a database helper class to create the database through SQLite.
        return true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        //https://developer.android.com/training/data-storage/sqlite.html
        String newSelection = "key = "+"'"+selection+"'";                                              // This is the key which is to be queried in the form of 'key = selection'
        System.out.println(selection);
        System.out.println(newSelection);
        SQLiteDatabase messenger_db = messengerSQLHelper.getReadableDatabase();                         //Creates a Readable Database where we have inserted the key, value pairs.

        /* Here we query the selection in the table name given in the database.
            It returns a cursor which has the corresponding key, value pair needed.
         */
        Cursor cur = messenger_db.query(FeedReaderContract.FeedEntry.TABLE_NAME,projection, newSelection, null, null, null, sortOrder);
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        Log.v("query", selection);
        return cur;
    }
}
