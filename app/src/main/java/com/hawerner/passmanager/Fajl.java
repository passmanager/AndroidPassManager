package com.hawerner.passmanager;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class Fajl {
    public static void writeToFile(String fileName, String data, Context context){
        FileOutputStream outputStream;

        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.i("T", e.toString());
            e.printStackTrace();
        }
    }

    public static String readFromFile(String fileName, Context context){
        FileInputStream inputStream;
        String tmp = "";

        try {
            inputStream = context.openFileInput(fileName);
            int content;
            while ((content = inputStream.read()) != -1) {
                tmp += (char) content;
            }
            inputStream.close();
        }
        catch (FileNotFoundException e){

        }
        catch (IOException e1){

        }
        return tmp;
    }

    public static void appendToFile(String fileName, String stringToAppend, Context context){
        Fajl.writeToFile(fileName, Fajl.readFromFile(fileName, context) + stringToAppend, context);
    }

    public static Boolean fileExists(String fileName, Context context){
        try {
            FileInputStream inputStream = context.openFileInput(fileName);
        }
        catch (FileNotFoundException e){
            return false;
        }
        return true;
    }

}
