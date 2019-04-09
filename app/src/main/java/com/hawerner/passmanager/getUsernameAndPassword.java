package com.hawerner.passmanager;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.disposables.Disposable;

import static com.hawerner.passmanager.ListActivity.bin2hex;
import static com.hawerner.passmanager.ListActivity.getHash;


public class getUsernameAndPassword extends AppCompatActivity {

    private String key = "";
    boolean shouldContinue = false;
    String keyHash, salt;
    boolean showAd;
    private EditText keyInput;
    String keyName = "key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Preferences.init(getApplicationContext());
        if (Preferences.sharedPreferences.getBoolean(Preferences.darkMode, false)){
            setTheme(R.style.AppThemeDark);
        }
        else {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);

        try{
            key = getIntent().getStringExtra("key");
            if (key == null){
                key = "";
            }
            else{
                shouldContinue = true;
                setContentView(R.layout.activity_password_list);
                setDarkModeSwitch();
                this.loadList();
                return;
            }
        }catch (Exception ignored){
            key = "";
        }
        setContentView(R.layout.activity_login);

        keyInput = findViewById(R.id.enterkey);
        keyInput.setText("");
        keyInput.requestFocus();
        FileInputStream inputStream;
        String tmp;
        //useAsMasterKey = false;
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
                            //useAsMasterKey = true;
                        }
                    }).show();
            //useAsMasterKey = true;
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
            shouldContinue = false;
        } catch (IOException e1) {
            shouldContinue = false;
        } catch (Exception e2) {
            Log.i("T", e2.toString());
            shouldContinue = false;
        }
        if (RxFingerprint.isAvailable(getUsernameAndPassword.this) && Fajl.fileExists("keyCrypted", getApplicationContext())) {
            Disposable disposable = RxFingerprint.decrypt(EncryptionMethod.RSA, this, keyName, Fajl.readFromFile("keyCrypted", getApplicationContext()))
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
                        }
                        Log.e("ERROR", "decrypt", throwable);
                    });
        }

    }

    public void login() {
        EditText keyInput = findViewById(R.id.enterkey);
        key = keyInput.getText().toString();
        Log.i("T", "login()");
        shouldContinue = true;

        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(keyInput.getApplicationWindowToken(), 0);
        } catch (Exception e) {
        }
        if (!getPassword(key, salt).equals(keyHash)) {
            shouldContinue = false;
            new AlertDialog.Builder(this)
                    .setTitle("Master key not correct")
                    .setMessage("Key you entered isn't correct").show();
        }
        if (shouldContinue) {
            loadList();
        }
    }

    private String getPassword(String password, String salt) {
        String keytmp = bin2hex(getHash(key + salt));
        for (int i = 0; i < 512; ++i) {
            keytmp = bin2hex(getHash(keytmp + salt));
        }
        return keytmp;
    }

    private void copyDataBase()
    {
        try
        {
            InputStream myInput = getAssets().open("passread");
            String outFileName = "/data/data/" + getPackageName() + "/passread";
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

    //gotovo kopiranje
    String executeCommand(String cmd){
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
    String decrypt(String str, String key, String salt) {
        copyDataBase();
        //Toast.makeText(this,"Copied exe", Toast.LENGTH_LONG).show();
        executeCommand("/system/bin/chmod 744 /data/data/" + getPackageName() + "/passread");
        //Toast.makeText(this,"Changed permissions", Toast.LENGTH_LONG).show();
        //Toast.makeText(this,"/data/data/" + getPackageName() + "/passread " + salt + " " + str + " " + key, Toast.LENGTH_LONG).show();
        String decrypted = executeCommand("/data/data/" + getPackageName() + "/passread " + salt + " " + str + " " + key);
        //Toast.makeText(this,"Command executed", Toast.LENGTH_LONG).show();
        //String decrypted = "tetetetetete";
        return decrypted;
        //return decrypted;
    }

    private void loadList(){

        final File dir = new File(Environment.getExternalStorageDirectory(), "/Passwords/");
        if (!dir.exists()) {
            dir.mkdir();
        }

        String fileWanted = getIntent().getStringExtra("URI");
        final List<String> files = new ArrayList<>();
        for (File file : dir.listFiles()) {
            files.add(file.getName());
            if (file.getName().equals(fileWanted)){
                Log.i("T", "onItemClick started");
                BufferedReader br;
                final File file1 = new File(dir, fileWanted);
                try {
                    br = new BufferedReader(new FileReader(file1));
                } catch (FileNotFoundException e) {
                    Log.d("not found", "", e);
                    finish();
                    return;
                }
                try {
                    List<String> lines = new ArrayList<>();
                    String line = br.readLine();
                    while (line != null) {
                        lines.add(line);
                        line = br.readLine();
                    }
                    Log.i("T", "file read done");
                    String usernameSalt = lines.get(0);
                    String username = decrypt(lines.get(1), key, usernameSalt);
                    String passwordSalt = lines.get(2);
                    String password = decrypt(lines.get(3), key, passwordSalt);

                    Log.i("T", "making intent started");
                    Intent output = new Intent();
                    output.putExtra("username", username);
                    output.putExtra("password", password);
                    Log.i("T", username);
                    Log.i("T", "returning");
                    boolean isAccessibility = getIntent().getBooleanExtra("isAccessibility", false);
                    if (isAccessibility) {
                        Intent intent = new Intent(getUsernameAndPassword.this, MyAccessibilityService.class);
                        intent.setAction("com.hawerner.passmanager.MyAccessibilityService");
                        intent.putExtra("username", username);
                        intent.putExtra("password", password);
                        startService(intent);
                    }
                    setResult(Activity.RESULT_OK, output);
                    finish();
                }catch (IOException e) {
                    Log.e("reading file", "", e);
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                    return;
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        Log.e("closing reader", "", e);
                    }
                }
                shouldContinue = false;
                finish();
                return;
            }
        }
        if (shouldContinue) {
            setContentView(R.layout.activity_password_list);
            setDarkModeSwitch();
            //this.onResume();
            Collections.sort(files, String.CASE_INSENSITIVE_ORDER);
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, files);
            final ListView list = findViewById(R.id.filesListView);
            list.setAdapter(adapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Log.i("T", "onItemClick started");
                    BufferedReader br;
                    final File file = new File(dir, files.get(i));
                    try {
                        br = new BufferedReader(new FileReader(file));
                    } catch (FileNotFoundException e) {
                        Log.d("not found", "", e);
                        finish();
                        return;
                    }
                    try {
                        List<String> lines = new ArrayList<>();
                        String line = br.readLine();
                        while (line != null) {
                            lines.add(line);
                            line = br.readLine();
                        }
                        Log.i("T", "file read done");
                        String usernameSalt = lines.get(0);
                        String username = decrypt(lines.get(1), key, usernameSalt);
                        String passwordSalt = lines.get(2);
                        String password = decrypt(lines.get(3), key, passwordSalt);

                        Log.i("T", "making intent started");
                        Intent output = new Intent();
                        output.putExtra("username", username);
                        output.putExtra("password", password);
                        Log.i("T", username);
                        Log.i("T", "returning");
                        boolean isAccessibility = getIntent().getBooleanExtra("isAccessibility", false);
                        if (isAccessibility) {
                            Intent intent = new Intent(getUsernameAndPassword.this, MyAccessibilityService.class);
                            intent.setAction("com.hawerner.passmanager.MyAccessibilityService");
                            intent.putExtra("username", username);
                            intent.putExtra("password", password);
                            startService(intent);
                        }
                        setResult(Activity.RESULT_OK, output);
                        finish();

                    } catch (IOException e) {
                        Log.e("reading file", "", e);
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                    } finally {
                        try {
                            br.close();
                        } catch (IOException e) {
                            Log.e("closing reader", "", e);
                        }
                    }

                    finish();
                }
            });
        }
    }

    public void loginButton(View view) {
        login();
    }

    public void AddAccount(View view) {
        Intent intent = new Intent(getUsernameAndPassword.this, AddAccount.class);
        intent.putExtra("URI", getIntent().getStringExtra("URI"));
        intent.putExtra("key", key);
        intent.putExtra("isAccessibility", true);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK){
            Intent intent = new Intent(getUsernameAndPassword.this, MyAccessibilityService.class);
            String username = data.getStringExtra("username");
            String password = data.getStringExtra("password");
            intent.setAction("com.hawerner.passmanager.MyAccessibilityService");
            intent.putExtra("username", username);
            intent.putExtra("password", password);
            intent.putExtra("isAccessibility", true);
            startService(intent);
            Log.i("Accessibility", "Logging something...");
            finish();
        }
        else{
            Log.i("Accessibility", "Logging something else");
        }
    }

    public void setDarkModeSwitch(){
        Preferences.init(getApplicationContext());
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

    private void disableDarkMode() {
        SharedPreferences.Editor pref = Preferences.sharedPreferences.edit();
        pref.putBoolean(Preferences.darkMode, false);
        pref.apply();
        setTheme(R.style.AppTheme);
        this.reload();
    }

    private void enableDarkMode() {
        SharedPreferences.Editor pref = Preferences.sharedPreferences.edit();
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
        intent.putExtra("URI", getIntent().getStringExtra("URI"));
        intent.putExtra("isAccessibility", getIntent().getBooleanExtra("isAccessibility", false));
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

}


