package com.hawerner.passmanager;

import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
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
    public static final String URL_TABLE = "URL_TABLE";
    public static final String PACKAGE_TABlE = "PACKAGE_TABLE";
    private static final String TAG = "DBHelper";


    public DBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + ENTRY_TABLE + " ( id INTEGER primary key AUTOINCREMENT, name TEXT unique, usernameSalt TEXT, username TEXT, passwordSalt TEXT, password TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + URL_TABLE + " ( entryId INTEGER, url TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + PACKAGE_TABlE + " (entryId INTEGER, packageName TEXT);");
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
        db.delete(URL_TABLE, "entryId = ?", new String[] { id });
        db.delete(PACKAGE_TABlE, "entryId = ?", new String[] { id });
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

    public void addPackageName(String id, String packageName) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("entryId", id);
        contentValues.put("packageName", packageName);

        Cursor res = db.rawQuery("SELECT * FROM " + PACKAGE_TABlE + " where entryId = ? and packageName = ?", new String[] {id, packageName});

        if (res.getCount() != 0){
            res.close();
            return;
        }
        res.close();

        try {
            db.insertOrThrow(PACKAGE_TABlE, null, contentValues);
        }
        catch (Exception ignored){

        }
    }

    public List<String> getPackageNames(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        List<String> packageNames = new ArrayList<>();

        Cursor res = db.rawQuery("SELECT packageName from " + PACKAGE_TABlE + " where entryId = ?;", new String[]{ id });
        res.moveToFirst();

        while (res.isAfterLast() == false){
            packageNames.add(res.getString(res.getColumnIndex("packageName")));
            res.moveToNext();
        }
        res.close();

        return packageNames;
    }

    public List<String> getNamesByPackageName(String packageName){
        List<String> names = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor res = db.rawQuery("SELECT " + ENTRY_TABLE + ".name AS name FROM " + ENTRY_TABLE + ", " + PACKAGE_TABlE + " WHERE " + PACKAGE_TABlE + ".packageName = \"" + packageName + "\" AND " + ENTRY_TABLE + ".id = " + PACKAGE_TABlE + ".entryId;", null);
        res.moveToFirst();

        Log.i(TAG, String.valueOf(res.getCount()));

        while (res.isAfterLast() == false){
            names.add(res.getString(res.getColumnIndex("name")));
            res.moveToNext();
        }

        res.close();

        return names;

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
