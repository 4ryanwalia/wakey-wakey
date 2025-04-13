package com.nmims.wakeywakey;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.List;

public class RescheduleAlarmsWorker extends Worker {
    private static final String TAG = "RescheduleAlarmsWorker";

    public RescheduleAlarmsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Rescheduling alarms...");

        AlarmDatabase alarmDatabase = AlarmDatabase.getDatabase(getApplicationContext());
        AlarmScheduler scheduler = new AlarmScheduler(getApplicationContext());

        List<Alarm> alarms = (List<Alarm>) alarmDatabase.alarmDao().getAllAlarms();

        for (Alarm alarm : alarms) {
            if (alarm.isEnabled()) {
                Log.d(TAG, "Rescheduling alarm: ID=" + alarm.getId());

                // 🔥 Fix: Include ringtone URI in the schedule method
                scheduler.schedule(alarm.getId(), alarm.getHour(), alarm.getMinute(),
                        alarm.getRingtoneResourceId(), alarm.getRingtoneUri());
            }
        }

        return Result.success();
    }
}
