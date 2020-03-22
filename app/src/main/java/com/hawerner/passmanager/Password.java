package com.hawerner.passmanager;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Password {

    private final String TAG = "PasswordClass";

    private String name;
    private String key;

    private String usernameSalt;
    private String usernameC;
    private String username;

    private String passwordSalt;
    private String passwordC;
    private String password;

    private Context context;

    static {
        System.loadLibrary("crypt-lib");
    }


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

    private String generateSalt(){
        SecureRandom generator = new SecureRandom();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = 10;
        char tempChar;
        for (int i = 0; i < randomLength; i++) {
            tempChar = (char) (generator.nextInt(95) + 32);
            while (tempChar == '\'') tempChar = (char) (generator.nextInt(95) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    public void crypt(){
        if (usernameSalt == null)
            usernameSalt = generateSalt();
        if (passwordSalt == null)
            passwordSalt = generateSalt();

        usernameC = this.nativecrypt(username, usernameSalt, key);
        passwordC = this.nativecrypt(password, passwordSalt, key);

        Log.i("usernameSalt", usernameSalt);
        Log.i("usernameC", usernameC);
    }

    public void decrypt(){
        try {
            username = this.nativedecrypt(usernameC, usernameSalt, key);
            password = this.nativedecrypt(passwordC, passwordSalt, key);
        }
        catch (Exception e){
            Log.e("native", e.toString());
        }
    }

    public void create() throws NotUniqueException {
        if (usernameC == null || passwordC == null) this.crypt();

        DBHelper dbHelper = new DBHelper(context.getApplicationContext());
        try {
            dbHelper.add(name, usernameSalt, usernameC, passwordSalt, passwordC);
        } catch (DBHelper.NotUniqueException e) {
            throw new Password.NotUniqueException();
        }
    }

    public void save() {
        this.crypt();
        DBHelper dbHelper = new DBHelper(context);
        try{
            dbHelper.update(name, usernameSalt, usernameC, passwordSalt, passwordC);
        }
        catch (Exception ignored){}
    }

    public void load() throws DBHelper.doesNotExistException {
        DBHelper dbHelper = new DBHelper(context.getApplicationContext());
        List<String> lines;
        try {
            lines = dbHelper.getByName(name);
        } catch (DBHelper.doesNotExistException ignored) {
            //TODO: Password#load() doesNotExistException
            throw new DBHelper.doesNotExistException();
        }
        usernameSalt = lines.get(0);
        passwordSalt = lines.get(2);
        usernameC = lines.get(1);
        passwordC = lines.get(3);
        this.decrypt();
    }

    public void delete(){
        DBHelper dbHelper = new DBHelper(context.getApplicationContext());
        String id = null;
        try {
            id = dbHelper.getIdByName(name);
        } catch (DBHelper.doesNotExistException e) {
            Log.i(TAG, "Not found so no deleting :D");
            return;
        }
        dbHelper.delete(id);
    }

    public static List<String> getAllNames(Context context){
        /*final File dir = new File(Environment.getExternalStorageDirectory(), "/Passwords/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        final List<String> files = new ArrayList<>();
        for (File file : dir.listFiles()) {
            files.add(file.getName());
        }
        */
        DBHelper dbHelper = new DBHelper(context.getApplicationContext());
        final List<String> passwords = dbHelper.getAllNames();
        Collections.sort(passwords, String.CASE_INSENSITIVE_ORDER);
        return passwords;
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

            executeCommand("chmod 755 /data/data/" + context.getPackageName() + "/passgen");
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void copyPassread(){
        try
        {
            InputStream myInput = context.getAssets().open("passread");
            String outFileName = "/data/data/" + context.getPackageName() + "/passread";
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

            executeCommand("chmod 755 /data/data/" + context.getPackageName() + "/passread");
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

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public void addPackageName(String packageName){
        DBHelper dbHelper = new DBHelper(context.getApplicationContext());
        try {
            dbHelper.addPackageName(dbHelper.getIdByName(name), packageName);
        } catch (DBHelper.doesNotExistException e) {
            e.printStackTrace();
        }
    }

    public List<String> getPackageNames(){
        DBHelper dbHelper = new DBHelper(context.getApplicationContext());
        List<String> packageNames = null;
        try {
            packageNames = dbHelper.getPackageNames(dbHelper.getIdByName(name));
        } catch (DBHelper.doesNotExistException e) {
            return new ArrayList<String>();
        }
        return packageNames;
    }

    private String randomSalt(int length){
        StringBuilder str = new StringBuilder();
        Random random = new SecureRandom();
        char tempChar;
        for (int i = 0; i < length; ++i){
            tempChar = (char) (random.nextInt(95) + 32);
            while (tempChar == '\'') tempChar = (char) (random.nextInt(95) + 32);
            str.append(tempChar);
        }
        return str.toString();
    }

    private String randomSalt(){
        return randomSalt(10);
    }

    public class NotUniqueException extends Exception {
        public NotUniqueException() { super(); }
        public NotUniqueException(String msg) { super(msg); }
    }

    public native String nativedecrypt(String data, String salt, String key);

    public native String nativecrypt(String data, String salt, String key);

}
