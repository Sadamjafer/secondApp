package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // Accounts
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): Account?

    // Expenses
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)

    // Incomes
    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomes(): Flow<List<Income>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: Income)

    @Update
    suspend fun updateIncome(income: Income)

    @Delete
    suspend fun deleteIncome(income: Income)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteIncomeById(id: Int)

    // Bulk actions for backup / restore
    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Query("DELETE FROM incomes")
    suspend fun deleteAllIncomes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<Account>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomes(incomes: List<Income>)
}
