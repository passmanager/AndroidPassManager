package com.hawerner.passmanager;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Environment;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.sql.Array;
import java.text.CollationElementIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.mtramin.rxfingerprint.EncryptionMethod;
import com.mtramin.rxfingerprint.RxFingerprint;
import com.r0adkll.slidr.Slidr;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import io.reactivex.disposables.Disposable;

import static com.hawerner.passmanager.AddAccount.writeToFile;

public class ListActivity extends AppCompatActivity {
    private String key = "";
    boolean shouldContinue = false;
    boolean useAsMasterKey = false;
    boolean needToReEncrypt = false;
    private String keyHash, salt;
    InterstitialAd mInterstitialAd;
    boolean showAd;
    private EditText keyInput;
    String keyName = "key";
    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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


        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, getString(R.string.app_ad_unit_id));

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interestial_ad_unit_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        Random generator = new Random();
        if (generator.nextInt(3) == 0) {
            showAd = true;
        } else {
            showAd = false;
        }


        if (RxFingerprint.isAvailable(ListActivity.this) && Fajl.fileExists("keyCrypted", getApplicationContext())){
            disposable = RxFingerprint.decrypt(EncryptionMethod.RSA, this, keyName, Fajl.readFromFile("keyCrypted", getApplicationContext()))
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
                                    Toast.makeText(getApplicationContext(), "Fingerprint not recognized, try again!", Toast.LENGTH_LONG).show();
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
                                    Toast.makeText(getApplicationContext(), "Fingerprint not recognized, try again!", Toast.LENGTH_LONG).show();
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
            if ((true || showAd) && mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            }
            setContentView(R.layout.activity_main);
            this.onResume();
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

        if (!key.equals("")) {
            final File dir = new File(Environment.getExternalStorageDirectory(), "/Passwords/");
            if (!dir.exists()) {
                dir.mkdir();
            }

            setContentView(R.layout.activity_main);
            setTitle(R.string.app_name);

            final List<String> files = new ArrayList<>();
            for (File file : dir.listFiles()) {
                files.add(file.getName());
            }
            Collections.sort(files, String.CASE_INSENSITIVE_ORDER);
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, files);
            final ListView list = findViewById(R.id.filesListView);
            list.setAdapter(adapter);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Intent intent = new Intent(ListActivity.this, PasswordActivity.class);
                    intent.putExtra("file", files.get(i));
                    intent.putExtra("dir", dir.getAbsolutePath());
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
                            File fileToDelete = new File(Environment.getExternalStorageDirectory() + "/Passwords/", files.get(index));
                            if (fileToDelete.delete()) {
                                Log.v("long cliked", "fajl delited");
                            } else {
                                Log.v("long clicked", "fajl not delited");
                            }
                            //adapter.remove(files.get(index));
                            //list.deferNotifyDataSetChanged();
                            //files.remove(index);
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
            // Gets the ad view defined in layout/ad_fragment.xml with ad unit ID set in
            // values/strings.xml.
            try {
                AdView adView = findViewById(R.id.ad_view);

                // Create an ad request. Check your logcat output for the hashed device ID to
                // get test ads on a physical device. e.g.
                // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
                //vuk tel 41CD0028630068A88813F641CAACD6C1
                AdRequest adRequest = new AdRequest.Builder()
                        .addTestDevice("41CD0028630068A88813F641CAACD6C1")
                        .build();

                // Start loading the ad in the background.
                adView.loadAd(adRequest);
            } catch (Exception e) {
            }
        }
        else if (disposable != null && disposable.isDisposed()) {
            disposable = RxFingerprint.decrypt(EncryptionMethod.RSA, this, keyName, Fajl.readFromFile("keyCrypted", getApplicationContext()))
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
}
