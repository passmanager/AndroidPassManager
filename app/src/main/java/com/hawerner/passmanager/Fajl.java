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
    public static void writeToFile(String fileName, String data){
        Log.i("T", data);
        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(data);

        }
        catch ( IOException e)
        {
            Log.e("writeToFile", "Writing to file failed " + fileName);
        }
        catch (Exception e){
            Log.e("writeToFile", "Writing to file failed " + fileName);
        }
        finally
        {
            try
            {
                if ( writer != null)
                    writer.close( );
            }
            catch ( IOException e)
            {
            }
        }
    }

    public static String readFromFile(String fileName){
        FileInputStream inputStream;
        StringBuilder tmp = new StringBuilder();

        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(fileName));
        } catch (FileNotFoundException e) {
            Log.d("not found", "", e);
            return null;
        }
        try {
            String line = br.readLine();
            while (line != null) {
                tmp.append(line).append("\n");
                line = br.readLine();
            }
        }
        catch (FileNotFoundException e){

        }
        catch (IOException e1){

        }
        return tmp.toString();
    }

    public static void appendToFile(String fileName, String stringToAppend){
        Fajl.writeToFile(fileName, Fajl.readFromFile(fileName) + stringToAppend);
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
