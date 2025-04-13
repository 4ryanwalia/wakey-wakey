package com.nmims.wakeywakey;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alarms")
public class Alarm implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "hour")
    private int hour;

    @ColumnInfo(name = "minute")
    private int minute;

    @ColumnInfo(name = "enabled")
    private boolean enabled;

    @ColumnInfo(name = "ringtone_resource_id")
    private int ringtoneResourceId;

    @ColumnInfo(name = "ringtone_name")
    private String ringtoneName;

    @ColumnInfo(name = "time_in_millis")
    private long timeInMillis;

    // Default Constructor (Required for Room)
    public Alarm() {}

    // Constructor
    public Alarm(int hour, int minute, boolean enabled, int ringtoneResourceId, String ringtoneName, long timeInMillis) {
        this.hour = hour;
        this.minute = minute;
        this.enabled = enabled;
        this.ringtoneResourceId = ringtoneResourceId;
        this.ringtoneName = ringtoneName;
        this.timeInMillis = timeInMillis;
    }

    private String ringtoneUri;

    public String getRingtoneUri() { return ringtoneUri; }
    public void setRingtoneUri(String ringtoneUri) { this.ringtoneUri = ringtoneUri; }

    // Parcelable Implementation
    protected Alarm(Parcel in) {
        id = in.readInt();
        hour = in.readInt();
        minute = in.readInt();
        enabled = in.readByte() != 0;
        ringtoneResourceId = in.readInt();
        ringtoneName = in.readString();
        timeInMillis = in.readLong();
    }

    public static final Creator<Alarm> CREATOR = new Creator<Alarm>() {
        @Override
        public Alarm createFromParcel(Parcel in) {
            return new Alarm(in);
        }

        @Override
        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(hour);
        dest.writeInt(minute);
        dest.writeByte((byte) (enabled ? 1 : 0));
        dest.writeInt(ringtoneResourceId);
        dest.writeString(ringtoneName);
        dest.writeLong(timeInMillis);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters
    public int getId() { return id; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }
    public boolean isEnabled() { return enabled; }
    public int getRingtoneResourceId() { return ringtoneResourceId; }
    public String getRingtoneName() { return ringtoneName; }
    public long getTimeInMillis() { return timeInMillis; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setHour(int hour) { this.hour = hour; }
    public void setMinute(int minute) { this.minute = minute; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setRingtoneResourceId(int ringtoneResourceId) { this.ringtoneResourceId = ringtoneResourceId; }
    public void setRingtoneName(String ringtoneName) { this.ringtoneName = ringtoneName; }
    public void setTimeInMillis(long timeInMillis) { this.timeInMillis = timeInMillis; }

    @Override
    public String toString() {
        return "Alarm{" +
                "id=" + id +
                ", hour=" + hour +
                ", minute=" + minute +
                ", enabled=" + enabled +
                ", ringtoneResourceId=" + ringtoneResourceId +
                ", ringtoneName='" + ringtoneName + '\'' +
                ", timeInMillis=" + timeInMillis +
                '}';
    }
}
