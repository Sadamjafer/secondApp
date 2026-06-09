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

    @Query("UPDATE accounts SET balance = balance + :amount, lastUpdated = :timestamp WHERE id = :accountId")
    suspend fun updateAccountBalanceAtomic(accountId: Int, amount: Double, timestamp: Long)

    @Delete
    suspend fun deleteAccount(account: Account)

    // Expenses
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("UPDATE expenses SET amount = (SELECT SUM(amount) FROM finance_transactions WHERE parentId = :expenseId AND parentType = 'EXPENSE'), date = :timestamp WHERE id = :expenseId")
    suspend fun refreshExpenseTotal(expenseId: Int, timestamp: Long)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // Incomes
    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomes(): Flow<List<Income>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: Income): Long

    @Update
    suspend fun updateIncome(income: Income)

    @Query("UPDATE incomes SET amount = (SELECT SUM(amount) FROM finance_transactions WHERE parentId = :incomeId AND parentType = 'INCOME'), date = :timestamp WHERE id = :incomeId")
    suspend fun refreshIncomeTotal(incomeId: Int, timestamp: Long)

    @Delete
    suspend fun deleteIncome(income: Income)

    // Transactions
    @Query("SELECT * FROM finance_transactions WHERE parentId = :parentId AND parentType = :parentType ORDER BY date DESC")
    fun getTransactionsForParent(parentId: Int, parentType: String): Flow<List<FinanceTransaction>>

    @Query("SELECT * FROM finance_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<FinanceTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinanceTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: FinanceTransaction)

    @Query("DELETE FROM finance_transactions WHERE parentId = :parentId AND parentType = :parentType")
    suspend fun deleteTransactionsByParent(parentId: Int, parentType: String)
}
