package com.hawerner.passmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.r0adkll.slidr.Slidr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class PasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (Fajl.readFromFile("darkMode", getApplicationContext()).equals("true")){
            setTheme(R.style.AppThemeDark);
        }
        else{
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_password);


        Slidr.attach(this);

        final Intent intent = getIntent();
        setTitle(intent.getStringExtra("file"));
        final String key = getIntent().getStringExtra("key");
        final File file = new File(intent.getStringExtra("dir"), intent.getStringExtra("file"));
        BufferedReader br;
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
            String usernameSalt = lines.get(0);
            String username = decrypt(lines.get(1), key, usernameSalt);
            username = username.replace("\u200b", " ");
            String passwordSalt = lines.get(2);
            String password = decrypt(lines.get(3), key, passwordSalt);
            password = password.replace("\u200b", " ");

            TextView userTextView = findViewById(R.id.usernameTextView);
            TextView pwdTextView = findViewById(R.id.passwordTextView);
            userTextView.setText(username);
            pwdTextView.setText(password);

            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData data = ClipData.newPlainText("Šifra", password);
            cm.setPrimaryClip(data);
            Snackbar.make(findViewById(R.id.passwordActivity), "Password has been copied to clipboard", Snackbar.LENGTH_LONG).show();

        } catch (IOException e) {
            Log.e("reading file", "", e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                Log.e("closing reader", "", e);
            }
        }
    }

    //jos malo kopiranja

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
}
