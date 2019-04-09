package com.hawerner.passmanager;

import android.content.Context;
import android.content.SharedPreferences;

public final class Preferences {
    public static final String sharedPreferencesName = "hawerner.passmanager";
    public static SharedPreferences sharedPreferences;


    public static final String darkMode = "darkMode";


    public static void init(Context context){
        if (sharedPreferences != null) return;

        sharedPreferences = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE);

    }

}
