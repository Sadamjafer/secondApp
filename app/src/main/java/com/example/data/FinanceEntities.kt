package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balance: Double = 0.0,
    val detail: String = "",
    val phone: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val category: String = "عام",
    val notes: String = ""
)

@Entity(tableName = "incomes")
data class Income(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val category: String = "عام",
    val notes: String = ""
)

@Entity(
    tableName = "finance_transactions",
    indices = [Index(value = ["parentId"])]
)
data class FinanceTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val parentId: Int,       // ID الخاص بـ Expense أو Income أو Account
    val parentType: String,  // "EXPENSE", "INCOME", "ACCOUNT"
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)
