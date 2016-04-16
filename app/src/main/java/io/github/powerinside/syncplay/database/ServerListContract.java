package io.github.powerinside.syncplay.database;

import android.provider.BaseColumns;

/**
 * Created by irfan on 4/4/16.
 */
public final class ServerListContract {
    public static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + ServerListContract.ServerEntry.TABLE_NAME
            + " (" + ServerEntry._ID + " INTEGER PRIMARY KEY," + ServerEntry.COLUMN_NAME_NAME + " TEXT UNIQUE NOT NULL,"
            + ServerEntry.COLUMN_NAME_ADDR + " TEXT NOT NULL," + ServerEntry.COLUMN_NAME_PASSWD + " TEXT,"
            + ServerEntry.COLUMN_NAME_DEFROOM + " TEXT NOT NULL," + ServerEntry.COLUMN_NAME_USERNAME + " TEXT NOT NULL )";
    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ServerEntry.TABLE_NAME;

    public ServerListContract() {
    }

    /* Inner class that defines the table contents */
    public static abstract class ServerEntry implements BaseColumns {
        public static final String TABLE_NAME = "serverList";
        public static final String COLUMN_NAME_ADDR = "address";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_PASSWD = "password";
        public static final String COLUMN_NAME_USERNAME = "username";
        public static final String COLUMN_NAME_DEFROOM = "room";
    }
}
