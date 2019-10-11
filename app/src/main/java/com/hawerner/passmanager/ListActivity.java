package com.hawerner.passmanager;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.mtramin.rxfingerprint.EncryptionMethod;
import com.mtramin.rxfingerprint.RxFingerprint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.reactivex.disposables.Disposable;

public class ListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private String key = "";
    boolean shouldContinue = false;
    boolean useAsMasterKey = false;
    boolean needToReEncrypt = false;
    private String keyHash, salt;
    private EditText keyInput;
    String keyName = "key";
    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Preferences.init(getApplicationContext());
        if (Preferences.sharedPreferences.getBoolean(Preferences.darkMode, false)) {
            setTheme(R.style.AppThemeDark);
        }
        else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (ContextCompat.checkSelfPermission(ListActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ListActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            this.onCreate1();
        }
        startService(new Intent(this, MyAccessibilityService.class));
    }

    protected void onCreate1() {
        key = "";

        try{
            key = getIntent().getStringExtra("key");
            if (key == null){
                key = "";
            }
            else{
                setContentView(R.layout.activity_password_list);
                setDarkModeSwitch();
                this.onResume();
                this.loadList();
                return;
            }
        }catch (Exception ignored){
            key = "";
        }

        Random generator = new SecureRandom();

        if (RxFingerprint.isAvailable(ListActivity.this) && Fajl.fileExists("keyCrypted", getApplicationContext())){
            disposable = RxFingerprint.decrypt(EncryptionMethod.RSA, this, keyName, Fajl.readFromFile("keyCrypted"))
                    .subscribe(decryptionResult -> {
                        switch (decryptionResult.getResult()) {
                            case FAILED:
                                Toast.makeText(getApplicationContext(), "Fingerprint not recognized, try again!", Toast.LENGTH_LONG).show();
                                break;
                            case HELP:
                                Toast.makeText(getApplicationContext(), decryptionResult.getMessage(), Toast.LENGTH_LONG).show();
                                break;
                            case AUTHENTICATED:
                                //Toast.makeText(getApplicationContext(), "decrypted:\n" + decryptionResult.getDecrypted(), Toast.LENGTH_LONG).show();
                                ((EditText) findViewById(R.id.enterkey)).setText(decryptionResult.getDecrypted());
                                login();
                                break;
                        }
                    }, throwable -> {
                        //noinspection StatementWithEmptyBody
                        if (RxFingerprint.keyInvalidated(throwable)) {
                            // The keys you wanted to use are invalidated because the user has turned off his
                            // secure lock screen or changed the fingerprints stored on the device
                            // You have to re-encrypt the data to access it
                            Toast.makeText(getApplicationContext(), "You will need to authenticate using password", Toast.LENGTH_LONG).show();
                            needToReEncrypt = true;
                        }
                        Log.e("ERROR", "decrypt", throwable);
                    });
        }
        else{
            if (RxFingerprint.isAvailable(ListActivity.this)){
                needToReEncrypt = true;
            }
            Log.i("Fingerprint", "Not available or file not found");
        }


        keyInput = findViewById(R.id.enterkey);
        keyInput.setText("");
        FileInputStream inputStream;
        String tmp;
        useAsMasterKey = false;
        try {
            inputStream = getApplicationContext().openFileInput("key");
            tmp = "";
            int content;
            Log.i("T", "otvorio input strim");
            while ((content = inputStream.read()) != -1) {
                tmp += (char) content;
            }
            Log.i("T", "Napravio tmp: " + tmp);
            inputStream.close();

            keyHash = tmp;
        } catch (FileNotFoundException e) {
            shouldContinue = false;
            new AlertDialog.Builder(this)
                    .setTitle("Master key not set")
                    .setMessage("Your next entry will be used as master key")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            useAsMasterKey = true;
                        }
                    }).show();
            useAsMasterKey = true;
        } catch (IOException e1) {
            shouldContinue = false;
        } catch (Exception e2) {
            Log.i("T", e2.toString());
            shouldContinue = false;
        }
        try {
            inputStream = getApplicationContext().openFileInput("salt");
            tmp = "";
            int content;
            Log.i("T", "otvorio input strim");
            while ((content = inputStream.read()) != -1) {
                tmp += (char) content;
            }
            Log.i("T", "Napravio tmp: " + tmp);
            inputStream.close();

            salt = tmp;
        } catch (FileNotFoundException e) {
            StringBuilder randomStringBuilder = new StringBuilder();
            int randomLength = 30;
            char tempChar;
            for (int i = 0; i < randomLength; i++) {
                tempChar = (char) (generator.nextInt(95) + 32);
                while (tempChar == '\'') tempChar = (char) (generator.nextInt(95) + 32);
                randomStringBuilder.append(tempChar);
            }
            salt = randomStringBuilder.toString();
            writeToFile("salt", salt);
        } catch (IOException e1) {
            shouldContinue = false;
        } catch (Exception e2) {
            Log.i("T", e2.toString());
            shouldContinue = false;
        }
    }

    public static byte[] getHash(String password) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        digest.reset();
        return digest.digest(password.getBytes());
    }

    public static String bin2hex(byte[] data) {
        return String.format("%0" + (data.length * 2) + "X", new BigInteger(1, data));
    }

    public void loginButton(View view){
        this.login();
    }

    public void login() {
        EditText keyInput = findViewById(R.id.enterkey);
        key = keyInput.getText().toString();
        Log.i("T", "login()");
        shouldContinue = true;

        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(keyInput.getApplicationWindowToken(), 0);
        } catch (Exception ignored) {
        }

        if (useAsMasterKey) {
            keyHash = getPassword(key, salt);
            writeToFile("key", keyHash);
            if (RxFingerprint.isAvailable(ListActivity.this)) {
                disposable = RxFingerprint.encrypt(EncryptionMethod.RSA, this, keyName, key)
                        .subscribe(encryptionResult -> {
                            switch (encryptionResult.getResult()) {
                                default:
                                    writeToFile("keyCrypted", encryptionResult.getEncrypted());
                                    break;
                            }
                        }, throwable -> {
                            Log.e("ERROR", "authenticate", throwable);
                        });
                disposable.dispose();
            }
        }
        if (!getPassword(key, salt).equals(keyHash)) {
            shouldContinue = false;
            new AlertDialog.Builder(this)
                    .setTitle("Master key not correct")
                    .setMessage("Key you entered isn't correct").show();
        }

        if (needToReEncrypt){
            if (RxFingerprint.isAvailable(ListActivity.this)) {
                disposable = RxFingerprint.encrypt(EncryptionMethod.RSA, this, keyName, key)
                        .subscribe(encryptionResult -> {
                            switch (encryptionResult.getResult()) {
                                default:
                                    writeToFile("keyCrypted", encryptionResult.getEncrypted());
                                    break;
                            }
                        }, throwable -> {
                            Log.e("ERROR", "authenticate", throwable);
                        });
                disposable.dispose();
            }
        }

        if (shouldContinue) {
            setContentView(R.layout.activity_password_list);
            /*FloatingActionButton fab = findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AddAccount(view);
                }
            });
            fab.bringToFront();*/
            setDarkModeSwitch();
            this.loadList();
        }
    }

    private String getPassword(String password, String salt) {
        String keytmp = bin2hex(getHash(key + salt));
        for (int i = 0; i < 512; ++i) {
            keytmp = bin2hex(getHash(keytmp + salt));
        }
        return keytmp;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onCreate1();
                } else {
                    //TODO
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    public void AddAccount(View V) {
        Intent i = new Intent(ListActivity.this, AddAccount.class);
        i.putExtra("key", key);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (key.equals("") && disposable != null && disposable.isDisposed()) {
            disposable = RxFingerprint.decrypt(EncryptionMethod.RSA, this, keyName, Fajl.readFromFile("keyCrypted"))
                    .subscribe(decryptionResult -> {
                        switch (decryptionResult.getResult()) {
                            case FAILED:
                                Toast.makeText(getApplicationContext(), "Fingerprint not recognized, try again!", Toast.LENGTH_LONG).show();
                                break;
                            case HELP:
                                Toast.makeText(getApplicationContext(), decryptionResult.getMessage(), Toast.LENGTH_LONG).show();
                                break;
                            case AUTHENTICATED:
                                //Toast.makeText(getApplicationContext(), "decrypted:\n" + decryptionResult.getDecrypted(), Toast.LENGTH_LONG).show();
                                ((EditText) findViewById(R.id.enterkey)).setText(decryptionResult.getDecrypted());
                                login();
                                break;
                        }
                    }, throwable -> {
                        //noinspection StatementWithEmptyBody
                        if (RxFingerprint.keyInvalidated(throwable)) {
                            // The keys you wanted to use are invalidated because the user has turned off his
                            // secure lock screen or changed the fingerprints stored on the device
                            // You have to re-encrypt the data to access it
                            Toast.makeText(getApplicationContext(), "You will need to authenticate using password", Toast.LENGTH_LONG).show();
                            needToReEncrypt = true;
                        }
                        Log.e("ERROR", "decrypt", throwable);
                    });
        }

    }

    protected void loadList(){
        if (!key.equals("")) {

            Preferences.init(getApplicationContext());
            if(Preferences.sharedPreferences.getBoolean(Preferences.darkMode, false)){
                setTheme(R.style.AppThemeDark);
            }
            else {
                setTheme(R.style.AppTheme);
            }

            final List<String> files = Password.getAllNames(getApplicationContext());
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, files);
            final ListView list = findViewById(R.id.filesListView);
            assert list != null;
            list.setAdapter(adapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Intent intent = new Intent(ListActivity.this, PasswordActivity.class);
                    intent.putExtra("file", files.get(i));
                    intent.putExtra("key", key);
                    startActivity(intent);
                }
            });
            list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int i, long l) {
                    final int index = i;
                    Log.v("long clicked", "pos: " + files.get(i));
                    AlertDialog.Builder alert = new AlertDialog.Builder(
                            ListActivity.this);
                    alert.setTitle("Delete " + files.get(index) + "?");
                    alert.setMessage("Are you sure to delete " + files.get(index) + "?");
                    alert.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Password entry = new Password(key, getApplicationContext());
                            entry.setName(files.get(index));
                            entry.delete();
                            adapter.remove(files.get(index));
                            list.deferNotifyDataSetChanged();

                            dialog.dismiss();

                        }
                    });
                    alert.setNegativeButton("NO", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();
                        }
                    });

                    alert.show();

                    return true;
                }
            });
        }
    }

    @Override
    protected void onPause() {
        if (disposable != null) {
            disposable.dispose();
        }
        super.onPause();
    }

    private void writeToFile(String fileName, String data) {
        try {
            FileOutputStream outputStream = getApplicationContext().openFileOutput(fileName, getApplicationContext().MODE_PRIVATE);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.e("T", e.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        //((Switch) findViewById(R.id.darkModeSwitch)).setChecked(isChecked);
        /*((Switch) findViewById(R.id.darkModeSwitch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                toggleDarkMode(compoundButton);
            }
        });*/

        return true;
    }

    public void setDarkModeSwitch(){
        boolean isChecked = Preferences.sharedPreferences.getBoolean(Preferences.darkMode, false);
        try {
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            RelativeLayout red = (RelativeLayout) navigationView.getMenu().getItem(0).getActionView();
            ((Switch) red.findViewById(R.id.darkModeSwitch)).setChecked(isChecked);
            ((Switch) red.findViewById(R.id.darkModeSwitch)).setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b){
                        enableDarkMode();
                    }
                    else{
                        disableDarkMode();
                    }
                }
            });
        }
        catch (Exception e){
            Log.v("NavigationDrawer", e.getClass().toString());
            Log.v("NavigationDrawer", e.getCause().toString());
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void toggleDarkMode(View view) {
        Switch switchCompat = (Switch) view;
        /*if (switchCompat.isChecked()){
            enableDarkMode();
        }
        else{
            disableDarkMode();
        }*/
        if (Preferences.sharedPreferences.getBoolean(Preferences.darkMode, false)){
            disableDarkMode();
        }
        else{
            enableDarkMode();
        }
    }

    private void disableDarkMode() {
        SharedPreferences.Editor pref =  Preferences.sharedPreferences.edit();
        pref.putBoolean(Preferences.darkMode, false);
        pref.apply();
        setTheme(R.style.AppTheme);
        this.reload();
    }

    private void enableDarkMode() {
        SharedPreferences.Editor pref =  Preferences.sharedPreferences.edit();
        pref.putBoolean(Preferences.darkMode, true);
        pref.apply();
        setTheme(R.style.AppThemeDark);
        this.reload();
    }

    private void reload() {
        //setVisibility(View.GONE);
        //mScrollView.setVisibility(View.VISIBLE);
        Intent intent = getIntent();
        intent.putExtra("key", key);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}
