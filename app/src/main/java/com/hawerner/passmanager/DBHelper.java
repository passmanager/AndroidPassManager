package com.hawerner.passmanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "passwords.db";
    public static final String ENTRY_TABLE = "ENTRY";
    public static final String URI_TABLE = "URI_TABLE";
    private static final String TAG = "DBHelper";


    public DBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + ENTRY_TABLE + " ( id INTEGER primary key AUTOINCREMENT, name TEXT unique, usernameSalt TEXT, username TEXT, passwordSalt TEXT, password TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + URI_TABLE + " ( entryId INTEGER, androidPackageName TEXT, url TEXT);");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        Log.i(TAG, "onUpgrade called, but ignored");
    }

    public void add(String name, String usernameSalt, String username, String passwordSalt, String password) throws NotUniqueException {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("usernameSalt", usernameSalt);
        contentValues.put("username", username);
        contentValues.put("passwordSalt", passwordSalt);
        contentValues.put("password", password);
        try {
            db.insertOrThrow(ENTRY_TABLE, null, contentValues);
        }
        catch (android.database.sqlite.SQLiteConstraintException e){
            Log.i(TAG, "Error adding to database");
            throw new NotUniqueException();
        }
    }

    public int numberOfRows(String TABLE_NAME){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, TABLE_NAME);
        return numRows;
    }

    public int delete (String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(URI_TABLE, "entryId = ?", new String[] { id });
        return db.delete(ENTRY_TABLE,"id = ?", new String[] { id });
    }

    public List<String> getAllNames(){
        ArrayList<String> array_list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select name from " + ENTRY_TABLE, null );
        res.moveToFirst();

        while(res.isAfterLast() == false){
            array_list.add(res.getString(res.getColumnIndex("name")));
            res.moveToNext();
        }
        res.close();
        return array_list;

    }

    public List<String> getByName(String name) throws doesNotExistException {
        List<String> entry = new ArrayList<>(4);
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor res = db.rawQuery("select * from " + ENTRY_TABLE + " where name = ?", new String[] { name });
        res.moveToFirst();

        if (res.getCount() != 1){
            res.close();
            Log.d(TAG, "getByName(): not found");
            throw new doesNotExistException();
        }

        entry.add(res.getString(res.getColumnIndex("usernameSalt")));
        entry.add(res.getString(res.getColumnIndex("username")));
        entry.add(res.getString(res.getColumnIndex("passwordSalt")));
        entry.add(res.getString(res.getColumnIndex("password")));

        res.close();

        return entry;
    }

    public String getIdByName(String name) throws doesNotExistException {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor res = db.rawQuery("select id from " + ENTRY_TABLE + " where name = ?", new String[] { name });
        res.moveToFirst();

        if (res.getCount() != 1){
            res.close();
            Log.d(TAG, "getByName(): not found");
            throw new doesNotExistException();
        }
        String id = res.getString(res.getColumnIndex("id"));
        res.close();
        return id;
    }

    static class NotUniqueException extends Exception{
        public NotUniqueException() { super(); }
        public NotUniqueException(String msg) { super(msg); }
    }

    static class doesNotExistException extends Exception {
        public doesNotExistException() { super(); }
        public doesNotExistException(String msg) { super(msg); }
    }
}
