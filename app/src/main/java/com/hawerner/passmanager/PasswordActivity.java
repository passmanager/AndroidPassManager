package com.hawerner.passmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.Log;
import android.widget.TextView;

import com.r0adkll.slidr.Slidr;


public class PasswordActivity extends AppCompatActivity {

    AppCompatImageView iconView;

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

        setContentView(R.layout.activity_password);

        iconView = (AppCompatImageView) findViewById(R.id.entry_icon);


        Slidr.attach(this);

        final Intent intent = getIntent();
        setTitle(intent.getStringExtra("file"));
        final String key = getIntent().getStringExtra("key");
        final String file = intent.getStringExtra("file");

        Password entry = new Password(key, getApplicationContext());

        entry.setName(file);
        entry.load();

        String username = entry.getUsername();
        String password = entry.getPassword();

        TextView userTextView = findViewById(R.id.usernameTextView);
        TextView pwdTextView = findViewById(R.id.passwordTextView);
        userTextView.setText(username);
        pwdTextView.setText(password);

        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText("Šifra", password);
        assert cm != null;
        cm.setPrimaryClip(data);
        Snackbar.make(findViewById(R.id.passwordActivity), "Password has been copied to clipboard", Snackbar.LENGTH_LONG).show();

        setIcon();

    }

    void setIcon(){
        //final String packageName = getPackageName(getIntent().getStringExtra("file"));
        final String packageName = null;
        if (packageName != null){
            Log.i("PasswordActivity", packageName);
            try{
                Drawable icon = getApplicationContext().getPackageManager().getApplicationIcon(packageName);
                iconView.setImageDrawable(icon);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        else{
            Preferences.init(getApplicationContext());
            if (Preferences.sharedPreferences.getBoolean(Preferences.darkMode, false)) {
                iconView.setImageResource(R.drawable.ic_lock_white);
            }
            else{
                iconView.setImageResource(R.drawable.ic_lock_black);
            }
        }
    }
}
