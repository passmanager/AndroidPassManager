package com.hawerner.passmanager;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Fajl {
    public static void writeToFile(String fileName, String data, Context context){
        try {
            context.getApplicationContext();
            FileOutputStream outputStream = context.getApplicationContext().openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.e("T", e.toString());
        }
    }

    public static String readFromFile(String fileName, Context context){
        FileInputStream inputStream;
        StringBuilder tmp = new StringBuilder();
        try {
            inputStream = context.getApplicationContext().openFileInput(fileName);
            int content;
            Log.i("T", "otvorio input strim");
            while ((content = inputStream.read()) != -1) {
                tmp.append((char) content);
            }
            Log.i("T", "Napravio tmp: " + tmp);
            inputStream.close();

            return tmp.toString();
        } catch (FileNotFoundException e) {
        }
        catch (IOException e1){

        }
        return null;
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
