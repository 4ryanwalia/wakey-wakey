package com.nmims.wakeywakey; // Changed package

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

// No local imports needed as AlarmService, AlarmActivity are in the same package now
// import com.nmims.wakeywakey.service.AlarmService; removed
// import com.nmims.wakeywakey.ui.AlarmActivity; removed


public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    // Keep constants accessible within the package
    static final String EXTRA_ALARM_ID = "com.nmims.wakeywakey.ALARM_ID";
    static final String EXTRA_RINGTONE_RES_ID = "com.nmims.wakeywakey.RINGTONE_RES_ID";
    static final String EXTRA_ALARM_TIME_STR = "com.nmims.wakeywakey.ALARM_TIME_STR";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");

        int alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1);
        int ringtoneResId = intent.getIntExtra(EXTRA_RINGTONE_RES_ID, -1);
        String alarmTimeStr = intent.getStringExtra(EXTRA_ALARM_TIME_STR);


        if (alarmId == -1 || ringtoneResId == -1) {
            Log.e(TAG, "Invalid alarm data received.");
            return;
        }

        // Start the Ringing Service
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra(EXTRA_RINGTONE_RES_ID, ringtoneResId);
        serviceIntent.putExtra(EXTRA_ALARM_ID, alarmId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // Start the Alarm Activity UI
        Intent alarmActivityIntent = new Intent(context, AlarmActivity.class);
        alarmActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        alarmActivityIntent.putExtra(EXTRA_ALARM_TIME_STR, alarmTimeStr);
        alarmActivityIntent.putExtra(EXTRA_ALARM_ID, alarmId);
        context.startActivity(alarmActivityIntent);

    }
}