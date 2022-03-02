package net.vornao.ddns.dome.handler

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import kotlin.jvm.Synchronized

class DatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_SQL)
        db.execSQL(CREATE_DEVICES)
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, i: Int, i1: Int) {
        // TODO
    }

    @Synchronized
    override fun close() {
        super.close()
        dbInstance = null
    }

    companion object {
        const val VERSION = 1
        const val DB_NAME = "domebase.db"
        const val HOUSES_TABLE = "houses"
        const val HOUSE_ID = "house_id"
        const val HOUSE_NAME = "house_name"
        const val RV_POSITION = "rv_position"
        const val DEVICE_TABLE = "devices"
        const val DEVICE_NAME = "name"
        const val DEVICE_id = "id"
        const val DEVICE_HOUSE = "house_r"
        const val DEVICE_TYPE = "type"
        const val DEVICE_INFO = "info"
        private var dbInstance: DatabaseHelper? = null

        // Singleton pattern to prevent leaks and increase reuse
        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): DatabaseHelper? {
            if (dbInstance == null) dbInstance = DatabaseHelper(context.applicationContext)
            return dbInstance
        }

        private const val CREATE_SQL = "CREATE TABLE houses ( " +
                "house_id int, " +
                "house_name varchar(511), " +
                "PRIMARY KEY (house_id));"
        private const val CREATE_DEVICES = ("CREATE TABLE devices (" + "id INTEGER PRIMARY KEY, "
                + "name TEXT, " + "info TEXT, " + "type TEXT, "
                + "house_r INTEGER, "
                + "rv_position INTEGER, "
                + "FOREIGN KEY (house_r) REFERENCES houses(house_id));")
    }
}