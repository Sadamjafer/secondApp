package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {
    // Accounts
    val allAccounts: Flow<List<Account>> = financeDao.getAllAccounts()

    suspend fun insertAccount(account: Account) {
        financeDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        financeDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: Account) {
        financeDao.deleteAccount(account)
    }

    // Expenses
    val allExpenses: Flow<List<Expense>> = financeDao.getAllExpenses()

    suspend fun insertExpense(expense: Expense) {
        financeDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: Expense) {
        financeDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        financeDao.deleteExpense(expense)
    }

    // Incomes
    val allIncomes: Flow<List<Income>> = financeDao.getAllIncomes()

    suspend fun insertIncome(income: Income) {
        financeDao.insertIncome(income)
    }

    suspend fun updateIncome(income: Income) {
        financeDao.updateIncome(income)
    }

    suspend fun deleteIncome(income: Income) {
        financeDao.deleteIncome(income)
    }

    // Bulk restoration
    suspend fun restoreBackup(
        accounts: List<Account>,
        expenses: List<Expense>,
        incomes: List<Income>,
        safeWithdrawals: List<SafeWithdrawal> = emptyList()
    ) {
        financeDao.deleteAllAccounts()
        financeDao.deleteAllExpenses()
        financeDao.deleteAllIncomes()
        financeDao.deleteAllWithdrawals()
        financeDao.insertAccounts(accounts)
        financeDao.insertExpenses(expenses)
        financeDao.insertIncomes(incomes)
        if (safeWithdrawals.isNotEmpty()) {
            financeDao.insertWithdrawals(safeWithdrawals)
        }
    }

    // Safe Withdrawals
    val allWithdrawals: Flow<List<SafeWithdrawal>> = financeDao.getAllWithdrawals()

    suspend fun insertWithdrawal(withdrawal: SafeWithdrawal) {
        financeDao.insertWithdrawal(withdrawal)
    }

    suspend fun updateWithdrawal(withdrawal: SafeWithdrawal) {
        financeDao.updateWithdrawal(withdrawal)
    }

    suspend fun deleteWithdrawal(withdrawal: SafeWithdrawal) {
        financeDao.deleteWithdrawal(withdrawal)
    }

    suspend fun deleteWithdrawalById(id: Int) {
        financeDao.deleteWithdrawalById(id)
    }
}
