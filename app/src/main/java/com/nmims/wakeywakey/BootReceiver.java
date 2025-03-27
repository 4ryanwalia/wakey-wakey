package com.nmims.wakeywakey; // Changed package

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// No local imports needed as AlarmDatabase, Alarm, AlarmScheduler are in the same package now
// import com.nmims.wakeywakey.database.AlarmDatabase; removed
// import com.nmims.wakeywakey.model.Alarm; removed
// import com.nmims.wakeywakey.util.AlarmScheduler; removed


import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED))) {
            Log.d(TAG, "Device Boot Completed/Locked Boot Completed. Rescheduling alarms.");

            AlarmDatabase.databaseWriteExecutor.execute(() -> {
                AlarmDatabase db = AlarmDatabase.getDatabase(context.getApplicationContext());
                List<Alarm> alarmsToReschedule = db.alarmDao().getAllEnabledAlarmsSync();

                if (alarmsToReschedule != null && !alarmsToReschedule.isEmpty()) {
                    AlarmScheduler scheduler = new AlarmScheduler(context);
                    for (Alarm alarm : alarmsToReschedule) {
                        Log.d(TAG, "Rescheduling alarm: ID=" + alarm.getId() + " Time=" + alarm.getHour() + ":" + alarm.getMinute());
                        // Recalculate trigger time based on current time before rescheduling
                        long triggerTime = AlarmScheduler.calculateTriggerTime(alarm.getHour(), alarm.getMinute());
                        alarm.setTimeInMillis(triggerTime); // Update alarm object
                        // Consider updating this in the DB as well if time might change significantly
                        // db.alarmDao().update(alarm); // Uncomment if persistence of new trigger time is crucial
                        scheduler.schedule(alarm);
                    }
                    Log.i(TAG, alarmsToReschedule.size() + " alarms rescheduled.");
                } else {
                    Log.i(TAG, "No enabled alarms found to reschedule.");
                }
            });
        }
    }
}