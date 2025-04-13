package com.nmims.wakeywakey;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SetAlarmActivity extends AppCompatActivity {

    private static final String TAG = "SetAlarmActivity";
    static final String EXTRA_ALARM_ID = "com.nmims.wakeywakey.EXTRA_ALARM_ID";
    private static final int REQUEST_RINGTONE = 1;


    private Alarm selectedAlarm;

    private TimePicker timePicker;
    private Spinner ringtoneSpinner;
    private Button buttonSaveAlarm, buttonDeleteAlarm;
    private Toolbar toolbar;
    private AlarmScheduler alarmScheduler;
    private AlarmDatabase alarmDatabase;

    private Alarm currentAlarm = null;
    private int selectedRingtoneResId = R.raw.hey;
    private String selectedRingtoneName = "";

    private static class RingtoneOption {
        final String name;
        final int resourceId;

        RingtoneOption(String name, int resourceId) {
            this.name = name;
            this.resourceId = resourceId;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final RingtoneOption[] ringtoneOptions = {
            new RingtoneOption("Hey Ya", R.raw.hey),
            new RingtoneOption("Happy", R.raw.happyever),
            new RingtoneOption("Motivating", R.raw.motivational),
            new RingtoneOption("Irritating", R.raw.ahhhh),
            new RingtoneOption("Annoying", R.raw.annoykarduga)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm);

        toolbar = findViewById(R.id.toolbarSetAlarm);
        setSupportActionBar(toolbar);

        timePicker = findViewById(R.id.timePicker);
        ringtoneSpinner = findViewById(R.id.ringtoneSpinner);
        buttonSaveAlarm = findViewById(R.id.buttonSaveAlarm);
        buttonDeleteAlarm = findViewById(R.id.buttonDeleteAlarm);

        alarmScheduler = new AlarmScheduler(this);
        alarmDatabase = AlarmDatabase.getDatabase(this);

        int alarmId = getIntent().getIntExtra(EXTRA_ALARM_ID, -1);

        setupRingtoneSpinner();

        if (alarmId != -1) {
            loadAlarmData(alarmId);
            buttonSaveAlarm.setText(R.string.edit_alarm);
            buttonDeleteAlarm.setVisibility(View.VISIBLE);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.edit_alarm);
        } else {
            buttonDeleteAlarm.setVisibility(View.GONE);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.add_alarm);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        buttonSaveAlarm.setOnClickListener(v -> saveAlarm());
        buttonDeleteAlarm.setOnClickListener(v -> deleteAlarm());
    }

    private void setupRingtoneSpinner() {
        ArrayAdapter<RingtoneOption> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                ringtoneOptions);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark);
        ringtoneSpinner.setAdapter(adapter);
    }

    private void loadAlarmData(int alarmId) {
        AlarmDatabase.databaseWriteExecutor.execute(() -> {
            currentAlarm = alarmDatabase.alarmDao().getAlarmById(alarmId);
            runOnUiThread(() -> {
                if (currentAlarm != null) {
                    timePicker.setHour(currentAlarm.getHour());
                    timePicker.setMinute(currentAlarm.getMinute());
                    for (int i = 0; i < ringtoneOptions.length; i++) {
                        if (ringtoneOptions[i].resourceId == currentAlarm.getRingtoneResourceId()) {
                            ringtoneSpinner.setSelection(i);
                            selectedRingtoneResId = currentAlarm.getRingtoneResourceId();
                            selectedRingtoneName = currentAlarm.getRingtoneName();
                            break;
                        }
                    }
                } else {
                    Log.e(TAG, "Alarm not found: ID=" + alarmId);
                    Toast.makeText(this, "Error loading alarm.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void saveAlarm() {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        boolean enabled = true;

        // Get selected ringtone
        RingtoneOption selectedOption = (RingtoneOption) ringtoneSpinner.getSelectedItem();
        if (selectedOption != null) {
            selectedRingtoneResId = selectedOption.resourceId;
            selectedRingtoneName = selectedOption.name;
        }

        // 🔥 Ensure ringtone URI is saved properly
        String ringtoneUri = selectedAlarm != null ? selectedAlarm.getRingtoneUri() : null;

        if (ringtoneUri == null) {
            ringtoneUri = "default"; // Fallback in case ringtoneUri is null
        }

        long triggerTimeMillis = alarmScheduler.calculateTriggerTime(hour, minute);

        final Alarm alarmToSave;
        if (currentAlarm != null) {
            // Update existing alarm
            alarmToSave = currentAlarm;
            alarmToSave.setHour(hour);
            alarmToSave.setMinute(minute);
            alarmToSave.setEnabled(enabled);
            alarmToSave.setRingtoneResourceId(selectedRingtoneResId);
            alarmToSave.setRingtoneName(selectedRingtoneName);
            alarmToSave.setRingtoneUri(ringtoneUri);  // 🔥 Ensure ringtone URI is stored
            alarmToSave.setTimeInMillis(triggerTimeMillis);
        } else {
            // Create new alarm
            alarmToSave = new Alarm(hour, minute, enabled, selectedRingtoneResId, selectedRingtoneName, triggerTimeMillis);

        }

        AlarmDatabase.databaseWriteExecutor.execute(() -> {
            long resultId;
            if (currentAlarm != null) {
                alarmDatabase.alarmDao().update(alarmToSave);
                resultId = alarmToSave.getId();
            } else {
                resultId = alarmDatabase.alarmDao().insert(alarmToSave);
                alarmToSave.setId((int) resultId);
            }

            if (resultId > 0) {
                // Cancel old alarm and schedule new one
                alarmScheduler.cancel(alarmToSave.getId());
                alarmScheduler.schedule(alarmToSave.getId(), alarmToSave.getHour(), alarmToSave.getMinute(),
                        alarmToSave.getRingtoneResourceId(), alarmToSave.getRingtoneUri()); // 🔥 Pass the URI

                runOnUiThread(this::finish);
            } else {
                runOnUiThread(() -> Toast.makeText(SetAlarmActivity.this, "Error saving alarm.", Toast.LENGTH_SHORT).show());
            }
        });
    }



    private void deleteAlarm() {
        if (currentAlarm != null) {
            AlarmDatabase.databaseWriteExecutor.execute(() -> {
                alarmDatabase.alarmDao().delete(currentAlarm);
                Log.d(TAG, "Alarm deleted from DB: ID=" + currentAlarm.getId());
                runOnUiThread(() -> {
                    Toast.makeText(SetAlarmActivity.this, "Alarm deleted.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        } else {
            Toast.makeText(this, "No alarm to delete.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
