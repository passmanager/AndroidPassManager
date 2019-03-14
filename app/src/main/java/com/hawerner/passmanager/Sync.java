package com.hawerner.passmanager;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

public class Sync extends AsyncTask<String, Void, String> {

    public static final String getPassword = "getPassword";
    public static final String getAllPasswords = "getAllPasswords";

    private String link;
    private String user;

    public Sync(String link, String user) {
        this.link = link;
        this.user = user;
    }

    private void getPassword(String entryName){
        int count;
        try {
            Log.v("Sync", "poceo connect");
            URL url = new URL(link + "/user/" + user + "/" + entryName.replace(" ", "%20"));
            Log.v("Sync", "Parsirao link");
            URLConnection connection = url.openConnection();
            Log.v("Sync", (connection == null) ? "connection je null":"connection nije null");
            Log.v("Sync", "Otvorio vezu");
            assert connection != null;
            connection.setConnectTimeout(0);
            Log.v("Sync", "Povezao se");
            // Getting file length
            int lenghtOfFile = connection.getContentLength();
            Log.v("Sync", "Uzeo velicinu fajla");
            // Create a Input stream to read file - with 8k buffer
            InputStream input = new BufferedInputStream(url.openStream(), 8192);
            Log.v("Sync", "Otvorio stream");
            // Output stream to write file
            StringBuilder output = new StringBuilder();

            byte data[] = new byte[1024];
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            long total = 0;
            while ((line = reader.readLine()) != null) {
                total += line.length();
                // publishing the progress....
                // After this onProgressUpdate will be called
                //publishProgress("" + (int) ((total * 100) / lenghtOfFile));
                // writing data to file
                output.append(line);
                output.append("\n");
            }
            input.close();
            write(entryName, output.toString());
        }
        catch (SocketTimeoutException e) {
            //connectionTimeout=true;
        }
        catch (NullPointerException e){
            Log.e("Sync", "Desio se null pointer exception");
        }
        catch (Exception e) {
            Log.e("Sync", "" + e.getMessage());
        }
    }

    private void write(String entryName, String output) {
        Log.v("Sync", output);
        //Fajl.writeToFile(Environment.getExternalStorageDirectory().getPath() + "/" + entryName, output, context);
        AddAccount.writeToFile(Environment.getExternalStorageDirectory().getPath() + "/Passwords/" + entryName, output);
    }

    @Override
    protected String doInBackground(String... strings) {
        if (strings[0].equals(Sync.getPassword))    getPassword(strings[1]);
        if (strings[0].equals(Sync.getAllPasswords)) getAllPasswords();

        return null;
    }

    private void getAllPasswords() {
        int count;
        try {
            Log.v("Sync", "poceo connect");
            URL url = new URL(link + "/user/" + user + "/");
            Log.v("Sync", "Parsirao link");
            URLConnection connection = url.openConnection();
            Log.v("Sync", (connection == null) ? "connection je null":"connection nije null");
            Log.v("Sync", "Otvorio vezu");
            assert connection != null;
            connection.setConnectTimeout(0);
            Log.v("Sync", "Povezao se");
            // Getting file length
            int lenghtOfFile = connection.getContentLength();
            Log.v("Sync", "Uzeo velicinu fajla");
            // Create a Input stream to read file - with 8k buffer
            InputStream input = new BufferedInputStream(url.openStream(), 8192);
            Log.v("Sync", "Otvorio stream");
            // Output stream to write file
            StringBuilder output = new StringBuilder();

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            long total = 0;
            while ((line = reader.readLine()) != null) {
                total += line.length();
                // publishing the progress....
                // After this onProgressUpdate will be called
                //publishProgress("" + (int) ((total * 100) / lenghtOfFile));
                // writing data to file
                output.append(line);
                output.append("\n");
            }
            input.close();
            Log.v("Sync", output.toString());
            String[] entries = output.toString().split("\n");
            for (String entry : entries){
                getPassword(entry);
            }
        }
        catch (SocketTimeoutException e) {
            //connectionTimeout=true;
        }
        catch (NullPointerException e){
            Log.e("Sync", "Desio se null pointer exception");
        }
        catch (Exception e) {
            Log.e("Sync", "" + e.getMessage());
        }
    }
}
