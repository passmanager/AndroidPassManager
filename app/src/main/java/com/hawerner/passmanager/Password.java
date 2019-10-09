package com.hawerner.passmanager;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Password {

    private String name;
    private String key;

    private String usernameSalt;
    private String usernameC;
    private String username;

    private String passwordSalt;
    private String passwordC;
    private String password;

    private Context context;


    public Password(String key, Context context){
        this.context = context;
        name = null;
        this.key = key;

        usernameSalt = null;
        usernameC = null;
        username = null;

        passwordSalt = null;
        passwordC = null;
        password = null;
    }

    public void crypt(){

    }

    public void decrypt(){
        //Toast.makeText(this,"Copied exe", Toast.LENGTH_LONG).show();
        executeCommand("/system/bin/chmod 744 /data/data/" + context.getPackageName() + "/passread");
        username = executeCommand("/data/data/" + context.getPackageName() + "/passread " + usernameSalt + " " + usernameC + " " + key);
        password = executeCommand("/data/data/" + context.getPackageName() + "/passread " + passwordSalt + " " + passwordC + " " + key);
        //return decrypted;
    }

    public void save(){

        //TODO: nije nista uradjeno

        String data="";
        //logika
        copyPassgen();
        executeCommand("/system/bin/chmod 744 /data/data/" + context.getPackageName() + "/passgen");
        this.crypt();
        String usernameString = username;
        String passwordString = password;
        usernameString = usernameString.replace(" ", "\u200b");
        passwordString = passwordString.replace(" ", "\u200b");
        String cmd = "/data/data/" + context.getPackageName() + "/passgen " + usernameString + " " + passwordString + " " + key;
        data = executeCommand(cmd);
        //kraj logike
        //writeToFile(fileName.getText().toString(), data);
        Fajl.writeToFile(Environment.getExternalStorageDirectory().getPath() + "/Passwords/" + name.replace('/', '⁄'), data, context);
    }

    public void load(){
        copyPassgen();
        String data = Fajl.readFromFile(Environment.getExternalStorageDirectory().getPath() + "/Passwords/" + name.replace('/', '⁄'), context);
        List<String> lines = Arrays.asList(data.split("\n"));
        usernameSalt = lines.get(0);
        passwordSalt = lines.get(2);
        usernameC = lines.get(1);
        passwordC = lines.get(3);
        this.decrypt();
        username = username.replace("\u200b", " ");
        password = password.replace("\u200b", " ");
    }

    public static List<String> getAllNames(){
        final File dir = new File(Environment.getExternalStorageDirectory(), "/Passwords/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        final List<String> files = new ArrayList<>();
        for (File file : dir.listFiles()) {
            files.add(file.getName());
        }
        Collections.sort(files, String.CASE_INSENSITIVE_ORDER);
        return files;
    }

    public String getUsernameSalt() {
        return usernameSalt;
    }

    public void setUsernameSalt(String usernameSalt) {
        this.usernameSalt = usernameSalt;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getUsername(){
        return this.username;
    }

    public String getPassword(){
        return this.password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private void copyPassgen(){
        try
        {
            InputStream myInput = context.getAssets().open("passgen");
            String outFileName = "/data/data/" + context.getPackageName() + "/passgen";
            executeCommand("rm -f " + outFileName);
            OutputStream myOutput = new FileOutputStream(outFileName);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer))>0)
            {
                myOutput.write(buffer, 0, length);
            }

            myOutput.flush();
            myOutput.close();
            myInput.close();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private static String executeCommand(String cmd){
        try {
            // Executes the command.
            Process process = Runtime.getRuntime().exec(cmd);

            // Reads stdout.
            // NOTE: You can write to stdin of the command using
            //       process.getOutputStream().
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            // Waits for the command to finish.
            process.waitFor();

            return output.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
