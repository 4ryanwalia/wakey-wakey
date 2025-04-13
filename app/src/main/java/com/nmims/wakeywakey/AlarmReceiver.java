package com.nmims.wakeywakey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String EXTRA_ALARM_ID = "alarm_id";
    public static final String EXTRA_RINGTONE_URI = "ringtoneUri";

    // 🔥 Store WakeLock as a static variable
    public static PowerManager.WakeLock wakeLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "Alarm triggered!");

        String ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI);
        if (ringtoneUri == null) {
            ringtoneUri = "default"; // Default ringtone
        }

        int alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1);

        // Start MathProblemActivity
        Intent mathIntent = new Intent(context, MathProblemActivity.class);
        mathIntent.putExtra(EXTRA_RINGTONE_URI, ringtoneUri);
        mathIntent.putExtra(EXTRA_ALARM_ID, alarmId);
        mathIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(mathIntent);

        // Wake up the screen when the alarm rings
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "wakeywakey:alarmWakeLock");

            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        }
    }
}
