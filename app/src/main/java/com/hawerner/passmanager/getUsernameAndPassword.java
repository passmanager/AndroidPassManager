package com.hawerner.passmanager;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.mtramin.rxfingerprint.EncryptionMethod;
import com.mtramin.rxfingerprint.RxFingerprint;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.disposables.Disposable;

import static com.hawerner.passmanager.ListActivity.bin2hex;
import static com.hawerner.passmanager.ListActivity.getHash;


public class getUsernameAndPassword extends AppCompatActivity {

    private final String TAG = "getUsernameAndPassword";
    private String key = "";
    boolean shouldContinue = false;
    String keyHash, salt;
    boolean showAd;
    private EditText keyInput;
    String keyName = "key";
    boolean showingAll = false;
    boolean displayButton = false;
    List<String> allEntries = null;
    List<String> entries = new ArrayList<>();
    List<String> possibleEntries = new ArrayList<>();
    ArrayAdapter<String> adapter = null;
    ListView list = null;

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
                this.selectDataToLoadList();
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
            Disposable disposable = RxFingerprint.decrypt(EncryptionMethod.RSA, this, keyName, Fajl.readFromFile("keyCrypted"))
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
            selectDataToLoadList();
        }
    }

    private String getPassword(String password, String salt) {
        String keytmp = bin2hex(getHash(key + salt));
        for (int i = 0; i < 512; ++i) {
            keytmp = bin2hex(getHash(keytmp + salt));
        }
        return keytmp;
    }

    private void selectDataToLoadList(){
        //TODO: Use database to load
        String wantedPackageName = getIntent().getStringExtra("URI");
        allEntries = Password.getAllNames(getApplicationContext());
        Collections.sort(allEntries, String.CASE_INSENSITIVE_ORDER);
        if (wantedPackageName != null) {
            Password entry = new Password(key, getApplicationContext());
            try {
                DBHelper dbHelper = new DBHelper(getApplicationContext());
                possibleEntries = dbHelper.getNamesByPackageName(wantedPackageName);
                Collections.sort(possibleEntries, String.CASE_INSENSITIVE_ORDER);
                displayButton = true;

                if (possibleEntries.size() == 0) throw new Exception("Load all");

                if (possibleEntries.size() == 1) {
                    entry.setName(possibleEntries.get(0));
                    entry.load();
                    entry.decrypt();

                    String username = entry.getUsername();
                    String password = entry.getPassword();

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
                    shouldContinue = false;
                    finish();
                    return;
                }
                entries = possibleEntries;
                loadList();
            } catch (DBHelper.doesNotExistException ignored) {

            }
            catch (Exception e){
                if ("Load all".equals(e.getMessage())){
                    entries = allEntries;
                    displayButton = false;
                    loadList();
                }
            }
        }

    }

    public void loadList(){
        if (shouldContinue) {
            setContentView(R.layout.activity_password_list);
            setDarkModeSwitch();
            //this.onResume();
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, entries);
            list = findViewById(R.id.filesListView);

            list.setAdapter(adapter);
            list.deferNotifyDataSetChanged();
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    Password entry = new Password(key, getApplicationContext());
                    entry.setName(entries.get(i));
                    try {
                        entry.load();
                    } catch (DBHelper.doesNotExistException ignored) {
                        Log.i(TAG, "Entry not found, but should be since it's clicked from GUI");
                    }
                    entry.decrypt();

                    String username = entry.getUsername();
                    String password = entry.getPassword();
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
                }
            });
            if (displayButton) {
                ImageButton showAllButton = findViewById(R.id.button_show_more);
                showAllButton.setVisibility(View.VISIBLE);
                if (Preferences.sharedPreferences.getBoolean(Preferences.darkMode, false)) {
                    showAllButton.setImageResource(R.drawable.ic_arrow_down_white);
                }
            }
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

    public void changeEntries(View view){

        ImageButton button = findViewById(R.id.button_show_more);
        int down = R.drawable.ic_arrow_down_black;
        int up = R.drawable.ic_arrow_up_black;
        if (Preferences.sharedPreferences.getBoolean(Preferences.darkMode, false)){
            down = R.drawable.ic_arrow_down_white;
            up = R.drawable.ic_arrow_up_white;
        }

        if (showingAll){
            entries = possibleEntries;
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, entries);
            list.setAdapter(adapter);
            button.setImageResource(down);
            for (String i : entries){
                Log.i(TAG, i);
            }
        }
        else{
            entries = allEntries;
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, entries);
            list.setAdapter(adapter);
            button.setImageResource(up);
            for (String i : entries){
                Log.i(TAG, i);
            }
        }

        showingAll = !showingAll;
    }

}


