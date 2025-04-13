package com.nmims.wakeywakey;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface AlarmDao {

    // Insert a single alarm
    @Insert
    long insert(Alarm alarm);

    // Insert multiple alarms
    @Insert
    void insertAll(List<Alarm> alarms);

    // Update a single alarm
    @Update
    void update(Alarm alarm);

    // Update multiple alarms
    @Update
    void updateAll(List<Alarm> alarms);

    // Delete a single alarm
    @Delete
    void delete(Alarm alarm);

    // Delete all alarms
    @Query("DELETE FROM alarms")
    void deleteAll();

    // Get all alarms sorted by time (Fixed column name issue)
    @Query("SELECT * FROM alarms ORDER BY time_in_millis ASC")
    LiveData<List<Alarm>> getAllAlarms();

    // Get a specific alarm by ID
    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    Alarm getAlarmById(int alarmId);

    // Get only enabled alarms, sorted by time (Fixed column name issue)
    @Query("SELECT * FROM alarms WHERE enabled = 1 ORDER BY time_in_millis ASC")
    List<Alarm> getAllEnabledAlarmsSync();
}
