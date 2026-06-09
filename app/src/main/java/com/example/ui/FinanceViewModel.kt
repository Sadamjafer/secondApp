package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository

    val allAccounts: StateFlow<List<Account>>
    val allExpenses: StateFlow<List<Expense>>
    val allIncomes: StateFlow<List<Income>>
    val allTransactions: StateFlow<List<FinanceTransaction>>

    init {
        val dao = FinanceDatabase.getDatabase(application).financeDao()
        repository = FinanceRepository(dao)

        allAccounts = repository.allAccounts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allExpenses = repository.allExpenses
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allIncomes = repository.allIncomes
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allTransactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // Account operations
    fun addAccount(name: String, balance: Double = 0.0, detail: String = "", phone: String = "") {
        viewModelScope.launch {
            repository.insertAccount(Account(name = name, balance = balance, detail = detail, phone = phone))
        }
    }

    fun updateAccountBalance(account: Account, changeAmount: Double, note: String = "تعديل رصيد يدوي") {
        viewModelScope.launch {
            repository.updateAccountBalanceAtomic(account.id, changeAmount)
            repository.insertTransaction(
                FinanceTransaction(
                    parentId = account.id,
                    parentType = if (changeAmount >= 0) "ACCOUNT_IN" else "ACCOUNT_OUT",
                    amount = kotlin.math.abs(changeAmount),
                    date = System.currentTimeMillis(),
                    note = "${account.name}: $note"
                )
            )
        }
    }

    fun updateAccountDetails(account: Account, newName: String, newDetail: String = "", newPhone: String = "") {
        viewModelScope.launch {
            repository.updateAccount(account.copy(name = newName, detail = newDetail, phone = newPhone, lastUpdated = System.currentTimeMillis()))
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    // Expense operations
    fun addExpense(title: String, category: String = "عام") {
        viewModelScope.launch {
            repository.insertExpense(Expense(title = title, category = category))
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // Income operations
    fun addIncome(title: String, category: String = "عام") {
        viewModelScope.launch {
            repository.insertIncome(Income(title = title, category = category))
        }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            repository.deleteIncome(income)
        }
    }

    // Transaction operations
    fun addTransaction(parentId: Int, parentType: String, amount: Double, date: Long, note: String = "") {
        viewModelScope.launch {
            repository.insertTransaction(FinanceTransaction(parentId = parentId, parentType = parentType, amount = amount, date = date, note = note))
        }
    }

    // دالة خاصة لسحب الأرباح (الخصومات الشخصية)
    fun addWithdrawal(amount: Double, note: String, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertTransaction(
                FinanceTransaction(
                    parentId = 0, // 0 يعني سحب عام من الخزينة
                    parentType = "WITHDRAWAL",
                    amount = amount,
                    date = date,
                    note = note
                )
            )
        }
    }

    fun deleteTransaction(transaction: FinanceTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
