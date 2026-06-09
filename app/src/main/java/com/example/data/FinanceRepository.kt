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

    suspend fun updateAccountBalanceAtomic(accountId: Int, amount: Double) {
        financeDao.updateAccountBalanceAtomic(accountId, amount, System.currentTimeMillis())
    }

    suspend fun deleteAccount(account: Account) {
        financeDao.deleteAccount(account)
    }

    // Expenses
    val allExpenses: Flow<List<Expense>> = financeDao.getAllExpenses()

    suspend fun insertExpense(expense: Expense): Long {
        return financeDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: Expense) {
        financeDao.updateExpense(expense)
    }

    suspend fun refreshExpenseTotal(expenseId: Int) {
        financeDao.refreshExpenseTotal(expenseId, System.currentTimeMillis())
    }

    suspend fun deleteExpense(expense: Expense) {
        financeDao.deleteTransactionsByParent(expense.id, "EXPENSE")
        financeDao.deleteExpense(expense)
    }

    // Incomes
    val allIncomes: Flow<List<Income>> = financeDao.getAllIncomes()

    suspend fun insertIncome(income: Income): Long {
        return financeDao.insertIncome(income)
    }

    suspend fun updateIncome(income: Income) {
        financeDao.updateIncome(income)
    }

    suspend fun refreshIncomeTotal(incomeId: Int) {
        financeDao.refreshIncomeTotal(incomeId, System.currentTimeMillis())
    }

    suspend fun deleteIncome(income: Income) {
        financeDao.deleteTransactionsByParent(income.id, "INCOME")
        financeDao.deleteIncome(income)
    }

    // Transactions
    val allTransactions: Flow<List<FinanceTransaction>> = financeDao.getAllTransactions()

    fun getTransactionsForParent(parentId: Int, parentType: String): Flow<List<FinanceTransaction>> {
        return financeDao.getTransactionsForParent(parentId, parentType)
    }

    suspend fun insertTransaction(transaction: FinanceTransaction) {
        financeDao.insertTransaction(transaction)
        if (transaction.parentType == "EXPENSE") {
            refreshExpenseTotal(transaction.parentId)
        } else if (transaction.parentType == "INCOME") {
            refreshIncomeTotal(transaction.parentId)
        }
    }

    suspend fun deleteTransaction(transaction: FinanceTransaction) {
        financeDao.deleteTransaction(transaction)
        if (transaction.parentType == "EXPENSE") {
            refreshExpenseTotal(transaction.parentId)
        } else if (transaction.parentType == "INCOME") {
            refreshIncomeTotal(transaction.parentId)
        }
    }
}
