package com.nmims.wakeywakey; // Changed package

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "alarms")
public class Alarm implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int hour;
    public int minute;
    public boolean enabled;
    public int ringtoneResourceId;
    public String ringtoneName;
    public long timeInMillis;

    // Constructor
    public Alarm(int hour, int minute, boolean enabled, int ringtoneResourceId, String ringtoneName, long timeInMillis) {
        this.hour = hour;
        this.minute = minute;
        this.enabled = enabled;
        this.ringtoneResourceId = ringtoneResourceId;
        this.ringtoneName = ringtoneName;
        this.timeInMillis = timeInMillis;
    }

    // Getters (Needed for Room, Adapters etc.)
    public int getId() { return id; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }
    public boolean isEnabled() { return enabled; }
    public int getRingtoneResourceId() { return ringtoneResourceId; }
    public String getRingtoneName() { return ringtoneName; }
    public long getTimeInMillis() { return timeInMillis; }

    // Setters might be useful too, though constructor injection is often preferred
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setTimeInMillis(long timeInMillis) { this.timeInMillis = timeInMillis; }

}