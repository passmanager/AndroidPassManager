package com.hawerner.passmanager;

import android.content.Intent;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.security.SecureRandom;
import java.util.Random;

public class AddAccount extends AppCompatActivity {

    Button generatePassword, addAccount;
    EditText fileName, username, password;
    String key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Fajl.readFromFile("darkMode", getApplicationContext()).equals("true")){
            setTheme(R.style.AppThemeDark);
        }
        else{
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_account);

        generatePassword = (Button) findViewById(R.id.generatePassword);
        addAccount = (Button) findViewById(R.id.doneButton);
        fileName = (EditText) findViewById(R.id.fileName);
        username = (EditText) findViewById(R.id.userName);
        password = (EditText) findViewById(R.id.password);
        key = getIntent().getStringExtra("key");
        if (getIntent().getBooleanExtra("isAccessibility", false)){
            fileName.setText(getIntent().getStringExtra("URI"));
            Log.i("Intent", "dobio sam accessibility");
        }
    }
    public void generatePass(View V){
        password.setText("Generating password");
        Random generator = new SecureRandom();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = 30;
        char tempChar;
        for (int i = 0; i < randomLength; i++){
            tempChar = (char) (generator.nextInt(95) + 32);
            while (tempChar == '\'') tempChar = (char) (generator.nextInt(95) + 32);
            randomStringBuilder.append(tempChar);
        }
        password.setText(randomStringBuilder.toString());
    }

    public void AddAcc(View V){

        if (username.getText().toString().equals("") || password.getText().toString().equals("") || fileName.getText().toString().equals("")){
            Snackbar.make(findViewById(R.id.addAccountActivity), "You need to enter text into all text boxes", Snackbar.LENGTH_LONG).show();
            return;
        }

        String data="";
        //logika
        copyDataBase();
        executeCommand("/system/bin/chmod 744 /data/data/" + getPackageName() + "/passgen");
        data = executeCommand("/data/data/" + getPackageName() + "/passgen " + username.getText().toString() + " " + password.getText().toString() + " " + key);
        //Toast.makeText(this, data, Toast.LENGTH_LONG).show();
        //kraj logike
        //writeToFile(fileName.getText().toString(), data);
        Log.i("Sifra", data);
        writeToFile(Environment.getExternalStorageDirectory().getPath() + "/Passwords/" + fileName.getText().toString(), data);
        if (getIntent().getBooleanExtra("isAccessibility", false)){
            Intent output = new Intent();
            output.putExtra("username", username.getText().toString());
            output.putExtra("password", password.getText().toString());
            setResult(RESULT_OK, output);
        }
        finish();
    }

    private void copyDataBase()
    {
        try
        {
            InputStream myInput = getAssets().open("passgen");
            String outFileName = "/data/data/" + getPackageName() + "/passgen";
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

    static String executeCommand(String cmd){
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


    static public void writeToFile(String name, String data){
        Log.i("T", data);
        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter( name));
            writer.write(data);

        }
        catch ( IOException e)
        {
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
}
