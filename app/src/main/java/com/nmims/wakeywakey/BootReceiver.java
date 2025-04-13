package com.nmims.wakeywakey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED))) {
            Log.d(TAG, "Device Boot Completed. Rescheduling alarms using WorkManager.");

            // Use WorkManager to ensure task executes properly in the background
            WorkManager.getInstance(context)
                    .enqueue(OneTimeWorkRequest.from(RescheduleAlarmsWorker.class));
        }
    }
}
