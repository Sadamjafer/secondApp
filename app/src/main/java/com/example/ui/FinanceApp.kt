package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

enum class NavigationTab {
    Accounts, Expenses, Incomes, Treasury, Reports
}

@Composable
fun FinanceApp(viewModel: FinanceViewModel) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var currentTab by remember { mutableStateOf(NavigationTab.Treasury) }
        val context = LocalContext.current
        val activity = context as? androidx.activity.ComponentActivity
        var showExitConfirmDialog by remember { mutableStateOf(false) }

        androidx.activity.compose.BackHandler { showExitConfirmDialog = true }

        if (showExitConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showExitConfirmDialog = false },
                title = { Text("تأكيد الخروج", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من رغبتك في الخروج من التطبيق؟") },
                confirmButton = {
                    Button(onClick = { activity?.finish() }) { Text("خروج") }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirmDialog = false }) { Text("إلغاء") }
                }
            )
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.Accounts,
                        onClick = { currentTab = NavigationTab.Accounts },
                        label = { Text("الحسابات", fontSize = 10.sp) },
                        icon = { Icon(Icons.Default.List, null) }
                    )
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.Expenses,
                        onClick = { currentTab = NavigationTab.Expenses },
                        label = { Text("المصروفات", fontSize = 10.sp) },
                        icon = { Icon(Icons.Default.ShoppingCart, null) }
                    )
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.Incomes,
                        onClick = { currentTab = NavigationTab.Incomes },
                        label = { Text("الإيرادات", fontSize = 10.sp) },
                        icon = { Icon(Icons.Default.Star, null) }
                    )
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.Treasury,
                        onClick = { currentTab = NavigationTab.Treasury },
                        label = { Text("الخزينة", fontSize = 10.sp) },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, null) }
                    )
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.Reports,
                        onClick = { currentTab = NavigationTab.Reports },
                        label = { Text("التقارير", fontSize = 10.sp) },
                        icon = { Icon(Icons.Default.DateRange, null) }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentTab) {
                    NavigationTab.Accounts -> LedgerScreen(viewModel)
                    NavigationTab.Expenses -> ExpensesScreen(viewModel)
                    NavigationTab.Incomes -> IncomesScreen(viewModel)
                    NavigationTab.Treasury -> TreasuryScreen(viewModel)
                    NavigationTab.Reports -> ReportsScreen(viewModel)
                }
            }
        }
    }
}

// --- TREASURY SCREEN (الخزينة) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasuryScreen(viewModel: FinanceViewModel) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    var showWithdrawDialog by remember { mutableStateOf(false) }
    
    val today = Calendar.getInstance()
    val todayTransactions = transactions.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    val todayIn = todayTransactions.filter { it.parentType == "INCOME" || it.parentType == "ACCOUNT_IN" }.sumOf { it.amount }
    val todayOut = todayTransactions.filter { it.parentType == "EXPENSE" || it.parentType == "ACCOUNT_OUT" || it.parentType == "WITHDRAWAL" }.sumOf { it.amount }
    val netToday = todayIn - todayOut

    val totalIn = transactions.filter { it.parentType == "INCOME" || it.parentType == "ACCOUNT_IN" }.sumOf { it.amount }
    val totalOut = transactions.filter { it.parentType == "EXPENSE" || it.parentType == "ACCOUNT_OUT" }.sumOf { it.amount }
    val totalWithdrawals = transactions.filter { it.parentType == "WITHDRAWAL" }.sumOf { it.amount }
    val totalNet = totalIn - totalOut - totalWithdrawals

    val withdrawalsList = transactions.filter { it.parentType == "WITHDRAWAL" }.sortedByDescending { it.date }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("الخزينة المركزية", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(Date()), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("أرباح اليوم الصافية: ${Utils.formatAmount(netToday)}", fontWeight = FontWeight.Medium, color = if (netToday >= 0) Color(0xFF2E7D32) else Color.Red)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // أرباح الفترة والمسحوبات
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي الأرباح", fontSize = 12.sp)
                    Text(Utils.formatAmount(totalIn - totalOut), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي المسحوبات", fontSize = 12.sp, color = Color.Red)
                    Text(Utils.formatAmount(totalWithdrawals), fontWeight = FontWeight.Bold, color = Color.Red)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("الرصيد المتبقي في الخزينة", fontWeight = FontWeight.Medium)
                Text(Utils.formatAmount(totalNet), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { showWithdrawDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.RemoveCircleOutline, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تسجيل سحب جديد من الأرباح", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text("سجل المسحوبات والخصومات الشخصية", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, color = Color.Gray)
        
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(withdrawalsList) { txn ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(txn.note, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")).format(Date(txn.date)), fontSize = 11.sp, color = Color.Gray)
                    }
                    Text(Utils.formatAmount(txn.amount), fontWeight = FontWeight.ExtraBold, color = Color.Red, fontSize = 16.sp)
                    IconButton(onClick = { viewModel.deleteTransaction(txn) }) {
                        Icon(Icons.Default.Delete, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            }
        }
    }

    if (showWithdrawDialog) {
        var amt by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text("سحب مبلغ من الخزينة") },
            text = {
                Column {
                    OutlinedTextField(
                        value = amt, 
                        onValueChange = { amt = Utils.formatInputAmount(it) }, 
                        label = { Text("المبلغ المراد سحبه") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.Payments, null) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = note, 
                        onValueChange = { note = it }, 
                        label = { Text("البند (سبب السحب)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("مثلاً: مصروف شخصي، إيجار...") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val value = amt.replace(",", "").toDoubleOrNull() ?: 0.0
                    if (value > 0 && note.isNotBlank()) {
                        viewModel.addWithdrawal(value, note)
                        showWithdrawDialog = false
                    }
                }) { Text("تأكيد العملية") }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

// ... بقية الشاشات (LedgerScreen, ExpensesScreen, IncomesScreen, ReportsScreen) تظل كما هي ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(viewModel: FinanceViewModel) {
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(0) }

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf<Pair<Account, Boolean>?>(null) }
    var showEditDialog by remember { mutableStateOf<Account?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Account?>(null) }

    val filteredAccounts = accounts.filter {
        val match = it.name.contains(searchQuery, true)
        val filter = when (selectedFilter) {
            1 -> it.balance > 0
            2 -> it.balance < 0
            else -> true
        }
        match && filter
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                if (isSearchActive) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Text("الحسابات والديون", fontWeight = FontWeight.Bold)
                }
            },
            actions = {
                IconButton(onClick = { isSearchActive = !isSearchActive; searchQuery = "" }) {
                    Icon(if (isSearchActive) Icons.Default.Close else Icons.Default.Search, null)
                }
                IconButton(onClick = { showAddAccountDialog = true }) {
                    Icon(Icons.Default.AddCircle, null)
                }
            }
        )

        TabRow(selectedTabIndex = selectedFilter) {
            listOf("الكل", "مدينون", "دائنون").forEachIndexed { index, title ->
                Tab(selected = selectedFilter == index, onClick = { selectedFilter = index }, text = { Text(title) })
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredAccounts, key = { it.id }) { account ->
                    AccountItemRow(
                        account = account,
                        onAddClick = { showAdjustDialog = account to true },
                        onSubtractClick = { showAdjustDialog = account to false },
                        onEditClick = { showEditDialog = account },
                        onDeleteClick = { showDeleteDialog = account }
                    )
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }

        val total = accounts.sumOf { it.balance }
        Surface(tonalElevation = 8.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("إجمالي أرصدة العملاء:", fontWeight = FontWeight.Bold)
                Text(Utils.formatAmount(total), color = if (total >= 0) Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    if (showAddAccountDialog) {
        var name by remember { mutableStateOf("") }
        var bal by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("إضافة حساب") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") })
                    OutlinedTextField(value = bal, onValueChange = { bal = Utils.formatInputAmount(it) }, label = { Text("الرصيد") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addAccount(name, bal.replace(",", "").toDoubleOrNull() ?: 0.0)
                    showAddAccountDialog = false
                }) { Text("إضافة") }
            }
        )
    }

    key(showEditDialog?.id) {
        showEditDialog?.let { account ->
            var name by remember { mutableStateOf(account.name) }
            var phone by remember { mutableStateOf(account.phone) }
            AlertDialog(
                onDismissRequest = { showEditDialog = null },
                title = { Text("تعديل الحساب") },
                text = {
                    Column {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") })
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("الهاتف") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.updateAccountDetails(account, name, account.detail, phone)
                        showEditDialog = null
                    }) { Text("حفظ") }
                }
            )
        }
    }
    
    showAdjustDialog?.let { (account, isAdd) ->
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdjustDialog = null },
            title = { Text(if (isAdd) "إضافة لـ ${account.name}" else "خصم من ${account.name}") },
            text = {
                OutlinedTextField(value = amount, onValueChange = { amount = Utils.formatInputAmount(it) }, label = { Text("المبلغ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            },
            confirmButton = {
                Button(onClick = {
                    val value = amount.replace(",", "").toDoubleOrNull() ?: 0.0
                    viewModel.updateAccountBalance(account, if (isAdd) value else -value)
                    showAdjustDialog = null
                }) { Text("حفظ") }
            }
        )
    }

    showDeleteDialog?.let { account ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("حذف الحساب") },
            text = { Text("هل تريد حذف حساب ${account.name} نهائياً؟") },
            confirmButton = {
                Button(onClick = { viewModel.deleteAccount(account); showDeleteDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("حذف") }
            }
        )
    }
}

@Composable
fun AccountItemRow(account: Account, onAddClick: () -> Unit, onSubtractClick: () -> Unit, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().height(70.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onSubtractClick) { Icon(Icons.Default.RemoveCircle, null, tint = Color.Red) }
        IconButton(onClick = onAddClick) { Icon(Icons.Default.AddCircle, null, tint = Color.Green) }
        
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(account.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Utils.formatAmount(account.balance), color = if (account.balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.Bold)
        }
        
        Box {
            IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, null) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("تعديل") }, onClick = { expanded = false; onEditClick() })
                DropdownMenuItem(text = { Text("حذف") }, onClick = { expanded = false; onDeleteClick() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: FinanceViewModel) {
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("المصروفات") }, actions = {
            IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null) }
        })

        if (expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { showAddDialog = true }) { Text("إضافة أول مصروف") }
            }
        } else {
            LazyColumn {
                items(expenses) { expense ->
                    val expenseTransactions = transactions.filter { it.parentId == expense.id && it.parentType == "EXPENSE" }
                    ExpenseCardRow(expense, expenseTransactions, viewModel)
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("بند مصروفات جديد") },
            text = { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("الاسم") }) },
            confirmButton = {
                Button(onClick = { viewModel.addExpense(title); showAddDialog = false }) { Text("إضافة") }
            }
        )
    }
}

@Composable
fun ExpenseCardRow(expense: Expense, transactions: List<FinanceTransaction>, viewModel: FinanceViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    var showAddTxnDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { isExpanded = !isExpanded }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(expense.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(Utils.formatAmount(expense.amount), color = Color.Red, fontWeight = FontWeight.ExtraBold)
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    transactions.forEach { txn ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(txn.date)))
                            Text(Utils.formatAmount(txn.amount))
                            IconButton(onClick = { viewModel.deleteTransaction(txn) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Button(onClick = { showAddTxnDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("إضافة عملية")
                    }
                }
            }
        }
    }

    if (showAddTxnDialog) {
        var amt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTxnDialog = false },
            title = { Text("إضافة عملية لـ ${expense.title}") },
            text = { OutlinedTextField(value = amt, onValueChange = { amt = Utils.formatInputAmount(it) }, label = { Text("المبلغ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
            confirmButton = {
                Button(onClick = {
                    val value = amt.replace(",", "").toDoubleOrNull() ?: 0.0
                    viewModel.addTransaction(expense.id, "EXPENSE", value, System.currentTimeMillis())
                    showAddTxnDialog = false
                }) { Text("حفظ") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomesScreen(viewModel: FinanceViewModel) {
    val incomes by viewModel.allIncomes.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("الإيرادات") }, actions = {
            IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null) }
        })

        LazyColumn {
            items(incomes) { income ->
                val incomeTransactions = transactions.filter { it.parentId == income.id && it.parentType == "INCOME" }
                IncomeCardRow(income, incomeTransactions, viewModel)
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("بند إيرادات جديد") },
            text = { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("الاسم") }) },
            confirmButton = {
                Button(onClick = { viewModel.addIncome(title); showAddDialog = false }) { Text("إضافة") }
            }
        )
    }
}

@Composable
fun IncomeCardRow(income: Income, transactions: List<FinanceTransaction>, viewModel: FinanceViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    var showAddTxnDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { isExpanded = !isExpanded }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(income.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(Utils.formatAmount(income.amount), color = Color(0xFF2E7D32), fontWeight = FontWeight.ExtraBold)
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    transactions.forEach { txn ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(txn.date)))
                            Text(Utils.formatAmount(txn.amount))
                            IconButton(onClick = { viewModel.deleteTransaction(txn) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Button(onClick = { showAddTxnDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("إضافة عملية") }
                }
            }
        }
    }

    if (showAddTxnDialog) {
        var amt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTxnDialog = false },
            title = { Text("إضافة إيراد لـ ${income.title}") },
            text = { OutlinedTextField(value = amt, onValueChange = { amt = Utils.formatInputAmount(it) }, label = { Text("المبلغ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
            confirmButton = {
                Button(onClick = {
                    val value = amt.replace(",", "").toDoubleOrNull() ?: 0.0
                    viewModel.addTransaction(income.id, "INCOME", value, System.currentTimeMillis())
                    showAddTxnDialog = false
                }) { Text("حفظ") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    var startDate by remember { mutableStateOf(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L) }
    var endDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val filtered = transactions.filter { it.date in startDate..endDate }
    val totalIn = filtered.filter { it.parentType == "INCOME" || it.parentType == "ACCOUNT_IN" }.sumOf { it.amount }
    val totalOut = filtered.filter { it.parentType == "EXPENSE" || it.parentType == "ACCOUNT_OUT" || it.parentType == "WITHDRAWAL" }.sumOf { it.amount }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("التقارير المالية", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
                PdfHelper.generateAndSharePdf(context, startDate, endDate, filtered, totalIn, totalOut)
            }) {
                Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = startDate }
                DatePickerDialog(context, { _, y, m, d ->
                    cal.set(y, m, d)
                    startDate = cal.timeInMillis
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }, modifier = Modifier.weight(1f)) {
                Text("من: ${dateFormat.format(Date(startDate))}", fontSize = 11.sp)
            }
            Button(onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = endDate }
                DatePickerDialog(context, { _, y, m, d ->
                    cal.set(y, m, d)
                    endDate = cal.timeInMillis
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }, modifier = Modifier.weight(1f)) {
                Text("إلى: ${dateFormat.format(Date(endDate))}", fontSize = 11.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("إجمالي الإيرادات: ${Utils.formatAmount(totalIn)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Text("إجمالي المصروفات/المسحوبات: ${Utils.formatAmount(totalOut)}", color = Color.Red, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("صافي الربح للفترة: ${Utils.formatAmount(totalIn - totalOut)}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("سجل العمليات للفترة:", fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered.sortedByDescending { it.date }) { txn ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(when(txn.parentType) {
                        "INCOME" -> "إيراد"
                        "EXPENSE" -> "مصروف"
                        "ACCOUNT_IN" -> "تحصيل"
                        "ACCOUNT_OUT" -> "صرف"
                        "WITHDRAWAL" -> "سحب أرباح"
                        else -> txn.parentType
                    })
                    Text(Utils.formatAmount(txn.amount), color = if (txn.parentType.contains("IN")) Color(0xFF2E7D32) else Color.Red)
                    Text(SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(txn.date)))
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            }
        }
    }
}
