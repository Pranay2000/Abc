package com.example.assessment;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {

    private static final String
            PREF_BLOCK_HIDDEN_NUMBERS = "blockHiddenNumbers",
            PREF_NOTIFICATIONS = "notifications",
            PREF_BLOCK_OUT_OF_LIST = "block_out_of_list";

    private final SharedPreferences pref;

    public Settings(Context context) {
        pref = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
    }

    public boolean blockHiddenNumbers() {
        return pref.getBoolean(PREF_BLOCK_HIDDEN_NUMBERS, false);
    }

    public void blockHiddenNumbers(boolean block) {
        pref.edit()
                .putBoolean(PREF_BLOCK_HIDDEN_NUMBERS, block)
                .apply();
    }

    public boolean showNotifications() {
        return pref.getBoolean(PREF_NOTIFICATIONS, true);
    }

    public void showNotifications(boolean show) {
        pref.edit()
                .putBoolean(PREF_NOTIFICATIONS, show)
                .apply();
    }

    public boolean blockOutOfList() {
        return pref.getBoolean(PREF_BLOCK_OUT_OF_LIST, false);
    }

    public void blockOutOfList(boolean blockOutOfList) {
        pref.edit()
                .putBoolean(PREF_BLOCK_OUT_OF_LIST, blockOutOfList)
                .apply();
    }
}
