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
        Preferences.init(getApplicationContext());
        if (Preferences.sharedPreferences.getBoolean(Preferences.darkMode, false)){
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

        if (username.getText().toString().contains("'") || password.getText().toString().contains("'")){
            Snackbar.make(findViewById(R.id.addAccountActivity), "Please don't put ' in password or username, I was lazy programmer", Snackbar.LENGTH_LONG).show();
            return;
        }

        Password entry = new Password(key, getApplicationContext());

        entry.setName(fileName.getText().toString());
        entry.setUsername(username.getText().toString());
        entry.setPassword(password.getText().toString());
        entry.save();
        if (getIntent().getBooleanExtra("isAccessibility", false)){
            Intent output = new Intent();
            output.putExtra("username", username.getText().toString());
            output.putExtra("password", password.getText().toString());
            setResult(RESULT_OK, output);
        }
        finish();
    }
}
