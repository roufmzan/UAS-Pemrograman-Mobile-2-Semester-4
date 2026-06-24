package com.example.tugasku;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemeManager {
    private static final String PREF_NAME = "ThemePreferences";
    private static final String KEY_THEME = "theme_mode";
    
    public static final String THEME_DARK = "dark";
    public static final String THEME_LIGHT = "light";
    
    private SharedPreferences sharedPreferences;
    
    public ThemeManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    // Save theme preference
    public void saveTheme(String theme) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_THEME, theme);
        editor.apply();
    }
    
    // Load theme preference
    public String loadTheme() {
        return sharedPreferences.getString(KEY_THEME, THEME_DARK); // Default to dark
    }
    
    // Check if dark mode is enabled
    public boolean isDarkMode() {
        return THEME_DARK.equals(loadTheme());
    }
}
