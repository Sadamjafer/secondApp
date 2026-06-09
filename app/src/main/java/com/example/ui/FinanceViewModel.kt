package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Account
import com.example.data.Expense
import com.example.data.Income
import com.example.data.FinanceDatabase
import com.example.data.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository

    val allAccounts: StateFlow<List<Account>>
    val allExpenses: StateFlow<List<Expense>>
    val allIncomes: StateFlow<List<Income>>

    init {
        val dao = FinanceDatabase.getDatabase(application).financeDao()
        repository = FinanceRepository(dao)

        allAccounts = repository.allAccounts
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        allExpenses = repository.allExpenses
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        allIncomes = repository.allIncomes
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // Account operations
    fun addAccount(name: String, balance: Double = 0.0, detail: String = "", phone: String = "") {
        viewModelScope.launch {
            repository.insertAccount(
                Account(
                    name = name,
                    balance = balance,
                    detail = detail,
                    phone = phone,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateAccountBalance(account: Account, changeAmount: Double) {
        viewModelScope.launch {
            val updated = account.copy(
                balance = account.balance + changeAmount,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateAccount(updated)
        }
    }

    fun updateAccountDetails(account: Account, newName: String, newDetail: String = "", newPhone: String = "") {
        viewModelScope.launch {
            val updated = account.copy(
                name = newName,
                detail = newDetail,
                phone = newPhone,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateAccount(updated)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    // Expense operations
    fun addExpense(title: String, amount: Double, category: String = "عام", notes: String = "") {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    title = title,
                    amount = amount,
                    category = category,
                    notes = notes,
                    date = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun updateExpenseAmount(expense: Expense, newAmount: Double) {
        viewModelScope.launch {
            val updated = expense.copy(
                amount = newAmount
            )
            repository.updateExpense(updated)
        }
    }

    fun updateExpenseDetails(expense: Expense, newAmount: Double, newDate: Long, newNotes: String = expense.notes) {
        viewModelScope.launch {
            val updated = expense.copy(
                amount = newAmount,
                date = newDate,
                notes = newNotes
            )
            repository.updateExpense(updated)
        }
    }

    // Income operations
    fun addIncome(title: String, amount: Double, category: String = "عام", notes: String = "") {
        viewModelScope.launch {
            repository.insertIncome(
                Income(
                    title = title,
                    amount = amount,
                    category = category,
                    notes = notes,
                    date = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            repository.deleteIncome(income)
        }
    }

    fun updateIncomeAmount(income: Income, newAmount: Double) {
        viewModelScope.launch {
            val updated = income.copy(
                amount = newAmount
            )
            repository.updateIncome(updated)
        }
    }

    fun updateIncomeDetails(income: Income, newAmount: Double, newDate: Long, newNotes: String = income.notes) {
        viewModelScope.launch {
            val updated = income.copy(
                amount = newAmount,
                date = newDate,
                notes = newNotes
            )
            repository.updateIncome(updated)
        }
    }
}
