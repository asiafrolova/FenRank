package com.example.fencing_project.data.local



import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(
    entities = [LocalBout::class, LocalOpponent::class],
    version = 3,
    exportSchema = false
)
abstract class FencingDatabase : RoomDatabase() {
    abstract fun boutDao(): BoutDao
    abstract fun opponentDao(): OpponentDao

    companion object {
        @Volatile
        private var INSTANCE: FencingDatabase? = null

        fun getDatabase(context: android.content.Context): FencingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FencingDatabase::class.java,
                    "fencing_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}