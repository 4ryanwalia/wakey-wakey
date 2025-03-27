package com.nmims.wakeywakey; // Changed package

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

// No local imports needed as Alarm is in the same package now
// import com.nmims.wakeywakey.model.Alarm; removed

import java.util.List;

@Dao
public interface AlarmDao {

    @Insert
    long insert(Alarm alarm);

    @Update
    void update(Alarm alarm);

    @Delete
    void delete(Alarm alarm);

    @Query("SELECT * FROM alarms ORDER BY timeInMillis ASC")
    LiveData<List<Alarm>> getAllAlarms();

    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    Alarm getAlarmById(int alarmId);

    @Query("SELECT * FROM alarms WHERE enabled = 1 ORDER BY timeInMillis ASC")
    List<Alarm> getAllEnabledAlarmsSync();
}