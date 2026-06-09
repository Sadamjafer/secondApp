package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Account::class, Expense::class, Income::class], version = 2, exportSchema = false)
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
                        // Prepopulate database with screenshot accounts inside IO coroutine
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getDatabase(context).financeDao()
                            dao.insertAccount(Account(id = 1, name = "القبوة", balance = 0.0))
                            dao.insertAccount(Account(id = 2, name = "دقيق", balance = -7750000.0))
                            dao.insertAccount(Account(id = 3, name = "اونور", balance = -37000.0))
                            dao.insertAccount(Account(id = 4, name = "وجف", balance = 3798000.0))
                            dao.insertAccount(Account(id = 5, name = "حطب 6", balance = 1105000.0))
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
