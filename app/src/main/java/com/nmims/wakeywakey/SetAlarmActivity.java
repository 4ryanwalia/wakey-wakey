package com.nmims.wakeywakey; // Changed package
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.AdapterView;
import android.view.View;
import androidx.annotation.NonNull; // Added for NonNull
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
// No local imports needed as AlarmDatabase, Alarm, AlarmScheduler are in the same package nowm
public class SetAlarmActivity extends AppCompatActivity {

    private static final String TAG = "SetAlarmActivity";
    static final String EXTRA_ALARM_ID = "com.nmims.wakeywakey.EXTRA_ALARM_ID"; // Keep accessible within package

    private TimePicker timePicker;
    private Spinner ringtoneSpinner;
    private Button buttonSaveAlarm; // Changed from MaterialButton back to Button to match layout ID if needed
    private Toolbar toolbar; // Added reference for toolbar
    private AlarmScheduler alarmScheduler;
    private AlarmDatabase alarmDatabase;

    private Alarm currentAlarm = null;
    private int selectedRingtoneResId = R.raw.hey; // Default
    private String selectedRingtoneName = "";

    private static class RingtoneOption {
        final String name;
        final int resourceId;

        RingtoneOption(String name, int resourceId) {
            this.name = name;
            this.resourceId = resourceId;
        }

        @NonNull // Added NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    private final RingtoneOption[] ringtoneOptions = {
            new RingtoneOption("hey ya", R.raw.hey),
            new RingtoneOption("happy", R.raw.happyever),
            new RingtoneOption("motiviating", R.raw.motivational),
            new RingtoneOption("irritating", R.raw.ahhhh),
            new RingtoneOption("anoonying", R.raw.annoykarduga)
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if using the layout with CoordinatorLayout + Toolbar
        setContentView(R.layout.activity_set_alarm); // Assuming this layout has the toolbar

        toolbar = findViewById(R.id.toolbarSetAlarm); // Use the ID from the improved layout
        setSupportActionBar(toolbar);

        timePicker = findViewById(R.id.timePicker);
        ringtoneSpinner = findViewById(R.id.ringtoneSpinner);
        buttonSaveAlarm = findViewById(R.id.buttonSaveAlarm);

        alarmScheduler = new AlarmScheduler(this);
        alarmDatabase = AlarmDatabase.getDatabase(this);

        int alarmId = getIntent().getIntExtra(EXTRA_ALARM_ID, -1);

        setupRingtoneSpinner(); // Setup spinner first

        if (alarmId != -1) {
            loadAlarmData(alarmId);
            buttonSaveAlarm.setText(R.string.edit_alarm);
            if(getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.edit_alarm);
        } else {
            // Set default time or leave as default (often current time)
            if(getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.add_alarm);
        }
        // Enable back button
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }


        buttonSaveAlarm.setOnClickListener(v -> saveAlarm());

        ringtoneSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                RingtoneOption selectedOption = (RingtoneOption) parent.getItemAtPosition(position);
                selectedRingtoneResId = selectedOption.resourceId;
                selectedRingtoneName = selectedOption.name;
                Log.d(TAG, "Selected Ringtone: " + selectedRingtoneName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Fallback to default if needed
                if (!selectedRingtoneName.isEmpty()){ // Check if name already set by loadAlarmData
                    return;
                }
                RingtoneOption defaultOption = ringtoneOptions[0];
                selectedRingtoneResId = defaultOption.resourceId;
                selectedRingtoneName = defaultOption.name;
            }
        });
    }


    private void setupRingtoneSpinner() {
        ArrayAdapter<RingtoneOption> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, // Layout for the selected item
                ringtoneOptions);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark); // Layout for dropdown items
        ringtoneSpinner.setAdapter(adapter);
    }


    private void loadAlarmData(int alarmId) {
        AlarmDatabase.databaseWriteExecutor.execute(() -> {
            currentAlarm = alarmDatabase.alarmDao().getAlarmById(alarmId);
            runOnUiThread(() -> {
                if (currentAlarm != null) {
                    timePicker.setHour(currentAlarm.getHour());
                    timePicker.setMinute(currentAlarm.getMinute());
                    // Set spinner selection
                    for (int i = 0; i < ringtoneOptions.length; i++) {
                        if (ringtoneOptions[i].resourceId == currentAlarm.getRingtoneResourceId()) {
                            ringtoneSpinner.setSelection(i);
                            // Ensure these are updated if loaded after listener setup
                            selectedRingtoneResId = currentAlarm.getRingtoneResourceId();
                            selectedRingtoneName = currentAlarm.getRingtoneName();
                            break;
                        }
                    }
                } else {
                    Log.e(TAG, "Alarm with ID " + alarmId + " not found.");
                    Toast.makeText(this, "Error loading alarm.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void saveAlarm() {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        boolean enabled = true; // Default to enabled on save/update

        // Ensure ringtone name/ID are set (especially if nothing was selected)
        if (selectedRingtoneName.isEmpty()) {
            RingtoneOption selectedOption = (RingtoneOption) ringtoneSpinner.getSelectedItem();
            if (selectedOption != null) {
                selectedRingtoneResId = selectedOption.resourceId;
                selectedRingtoneName = selectedOption.name;
            } else {
                // Fallback if spinner is somehow empty
                RingtoneOption defaultOption = ringtoneOptions[0];
                selectedRingtoneResId = defaultOption.resourceId;
                selectedRingtoneName = defaultOption.name;
                Log.w(TAG, "Spinner item was null, falling back to default ringtone.");
            }
        }


        long triggerTimeMillis = AlarmScheduler.calculateTriggerTime(hour, minute); // Use helper

        final Alarm alarmToSave;
        if (currentAlarm != null) { // Editing
            alarmToSave = new Alarm(hour, minute, enabled,
                    selectedRingtoneResId, selectedRingtoneName, triggerTimeMillis);
            alarmToSave.id = currentAlarm.getId(); // IMPORTANT: Keep original ID
        } else { // Creating new
            alarmToSave = new Alarm(hour, minute, enabled, selectedRingtoneResId, selectedRingtoneName, triggerTimeMillis);
        }

        AlarmDatabase.databaseWriteExecutor.execute(() -> {
            long resultId;
            if (currentAlarm != null) {
                alarmDatabase.alarmDao().update(alarmToSave);
                resultId = alarmToSave.getId();
                Log.d(TAG, "Alarm updated in DB: ID=" + resultId);
            } else {
                resultId = alarmDatabase.alarmDao().insert(alarmToSave);
                alarmToSave.id = (int) resultId; // Set generated ID for scheduling
                Log.d(TAG, "Alarm inserted into DB: ID=" + resultId);
            }

            if (resultId > 0) {
                alarmScheduler.schedule(alarmToSave); // Schedule using the object with correct ID
                // Toast is handled by scheduler now
                runOnUiThread(this::finish); // Finish activity on UI thread
            } else {
                Log.e(TAG, "Failed to save alarm to database.");
                runOnUiThread(() -> Toast.makeText(SetAlarmActivity.this,
                        "Error saving alarm.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Standard behavior for back arrow
        return true;
    }
}