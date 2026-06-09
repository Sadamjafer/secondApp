package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Account::class, Expense::class, Income::class, FinanceTransaction::class],
    version = 3,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate using raw SQL to avoid circular dependency
                        db.execSQL("INSERT INTO accounts (id, name, balance, detail, phone, lastUpdated) VALUES (1, 'القبوة', 0.0, '', '', ${System.currentTimeMillis()})")
                        db.execSQL("INSERT INTO accounts (id, name, balance, detail, phone, lastUpdated) VALUES (2, 'دقيق', -7750000.0, '', '', ${System.currentTimeMillis()})")
                        db.execSQL("INSERT INTO accounts (id, name, balance, detail, phone, lastUpdated) VALUES (3, 'اونور', -37000.0, '', '', ${System.currentTimeMillis()})")
                        db.execSQL("INSERT INTO accounts (id, name, balance, detail, phone, lastUpdated) VALUES (4, 'وجف', 3798000.0, '', '', ${System.currentTimeMillis()})")
                        db.execSQL("INSERT INTO accounts (id, name, balance, detail, phone, lastUpdated) VALUES (5, 'حطب 6', 1105000.0, '', '', ${System.currentTimeMillis()})")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
