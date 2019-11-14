package com.hawerner.passmanager;

import android.content.DialogInterface;
import android.content.Intent;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AddAccount extends AppCompatActivity {

    Button generatePassword, addAccount;
    EditText fileName, username, password;
    String key;
    String packageName;
    PackageManager pm;

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

        pm = getApplicationContext().getPackageManager();

        generatePassword = (Button) findViewById(R.id.generatePassword);
        addAccount = (Button) findViewById(R.id.doneButton);
        fileName = (EditText) findViewById(R.id.fileName);
        username = (EditText) findViewById(R.id.userName);
        password = (EditText) findViewById(R.id.password);
        key = getIntent().getStringExtra("key");
        if (getIntent().getBooleanExtra("isAccessibility", false)){
            packageName = getIntent().getStringExtra("packageName");
            fileName.setText(getAppName(packageName));
            Log.i("Intent", "dobio sam accessibility");
        }
    }

    public String getAppName(String packageName){
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo( packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        return (String) (ai != null ? pm.getApplicationLabel(ai) : packageName);
    }

    public void getAllPackageNames(){
        List<ApplicationInfo> list = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> allPackageNames = new ArrayList<>(list.size());
        List<String> allAppNames = new ArrayList<>(allPackageNames.size());
        for (ApplicationInfo applicationInfo : list) {
            allPackageNames.add(applicationInfo.packageName);
            //allAppNames.add(pm.getApplicationLabel(applicationInfo).toString());
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
        try {
            entry.save();
            if (getIntent().getBooleanExtra("isAccessibility", false)){
                entry.addPackageName(packageName);
                Intent output = new Intent();
                output.putExtra("username", username.getText().toString());
                output.putExtra("password", password.getText().toString());
                setResult(RESULT_OK, output);
            }
            finish();
        } catch (Password.NotUniqueException e) {
            AlertDialog.Builder alert = new AlertDialog.Builder(AddAccount.this);
            alert.setTitle("Name taken");
            alert.setMessage("There is already password entry named " + entry.getName() + ".\nPlease change name or delete existing one");
            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alert.setCancelable(true);

            alert.create().show();
        }
    }
}
