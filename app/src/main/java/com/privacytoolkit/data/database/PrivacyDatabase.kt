package com.privacytoolkit.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room database for Privacy Toolkit.
 *
 * All data is stored locally on the device — no network calls are ever made.
 * This single-instance database holds the history for all three modules.
 */
@Database(
    entities = [AppScanResult::class, NetworkHistory::class, QRScanHistory::class],
    version = 1,
    exportSchema = false
)
abstract class PrivacyDatabase : RoomDatabase() {

    abstract fun appScanDao(): AppScanDao
    abstract fun networkHistoryDao(): NetworkHistoryDao
    abstract fun qrScanDao(): QRScanDao

    companion object {
        @Volatile
        private var INSTANCE: PrivacyDatabase? = null

        fun getInstance(context: Context): PrivacyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrivacyDatabase::class.java,
                    "privacy_toolkit_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
