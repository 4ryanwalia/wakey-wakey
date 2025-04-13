package com.nmims.wakeywakey;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Alarm.class}, version = 2, exportSchema = false) // UPDATED VERSION
public abstract class AlarmDatabase extends RoomDatabase {

    public abstract AlarmDao alarmDao();

    private static volatile AlarmDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Migration from version 1 to 2 (Rename column)
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // SQLite does not support renaming columns directly, so we need a workaround
            database.execSQL("ALTER TABLE alarms RENAME TO alarms_old;");
            database.execSQL(
                    "CREATE TABLE alarms (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "hour INTEGER NOT NULL, " +
                            "minute INTEGER NOT NULL, " +
                            "enabled INTEGER NOT NULL, " +
                            "ringtone_resource_id INTEGER NOT NULL, " +
                            "ringtone_name TEXT, " +
                            "time_in_millis INTEGER NOT NULL);"
            );
            database.execSQL(
                    "INSERT INTO alarms (id, hour, minute, enabled, ringtone_resource_id, ringtone_name, time_in_millis) " +
                            "SELECT id, hour, minute, enabled, ringtone_resource_id, ringtone_name, timeInMillis FROM alarms_old;"
            );
            database.execSQL("DROP TABLE alarms_old;");
        }
    };

    public static AlarmDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AlarmDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AlarmDatabase.class, "alarm_database")
                            .addMigrations(MIGRATION_1_2) // Apply migration
                            .fallbackToDestructiveMigration() // Optional: Remove old data if migration fails
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
