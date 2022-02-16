package net.vornao.ddns.dome.handler;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int    VERSION = 1;
    public static final String DB_NAME = "domebase.db";

    public static final String HOUSES_TABLE = "houses";
    public static final String HOUSE_ID = "house_id";
    public static final String HOUSE_NAME = "house_name";
    public static final String RV_POSITION = "rv_position";
    public static final String DEVICE_TABLE = "devices";
    public static final String DEVICE_NAME  = "name";
    public static final String DEVICE_id  = "id";
    public static final String DEVICE_HOUSE = "house_r";
    public static final String DEVICE_TYPE = "type";
    public static final String DEVICE_INFO = "info";

    private static DatabaseHelper dbInstance;

    // Singleton pattern to prevent leaks and increase reuse

    public static synchronized DatabaseHelper getInstance(@NonNull Context context) {
        if (dbInstance == null) dbInstance = new DatabaseHelper(context.getApplicationContext());
        return dbInstance;
    }

    private static final String CREATE_SQL =
            "CREATE TABLE houses ( " +
            "house_id int, " +
            "house_name varchar(511), " +
            "PRIMARY KEY (house_id));";

    private static final String CREATE_DEVICES =
            "CREATE TABLE devices (" + "id INTEGER PRIMARY KEY, "
            + "name TEXT, " + "info TEXT, " + "type TEXT, "
            + "house_r INTEGER, "
            + "rv_position INTEGER, "
            + "FOREIGN KEY (house_r) REFERENCES houses(house_id));";


    private DatabaseHelper(Context context){
        super(context, DB_NAME, null, VERSION);
    }


    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL(CREATE_SQL);
        db.execSQL(CREATE_DEVICES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            // TODO
    }

    public synchronized void close() {
        super.close();
        dbInstance = null;
    }
}
