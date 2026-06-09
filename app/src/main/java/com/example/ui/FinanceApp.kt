package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Account
import com.example.data.Expense
import com.example.data.Income
import java.text.SimpleDateFormat
import java.util.*

// Formatting numbers with commas as in the screenshot
fun formatAmount(value: Double): String {
    val isNegative = value < 0
    val absVal = kotlin.math.abs(value)
    val formatted = String.format("%,.0f", absVal)
    return if (isNegative) "-$formatted" else formatted
}

fun formatInputAmount(input: String): String {
    val clean = input.replace(",", "")
    if (clean.isEmpty()) return ""
    
    val isNegative = clean.startsWith("-")
    val content = if (isNegative) clean.substring(1) else clean
    
    if (content == "." || content.isEmpty()) {
        return clean
    }
    
    val parts = content.split(".")
    if (parts.size > 2) {
        return clean
    }
    
    val bg = parts[0]
    val dec = if (parts.size == 2) "." + parts[1] else ""
    
    if (!bg.all { it.isDigit() }) return clean
    
    val sb = StringBuilder()
    var count = 0
    for (i in bg.length - 1 downTo 0) {
        sb.append(bg[i])
        count++
        if (count % 3 == 0 && i > 0) {
            sb.append(",")
        }
    }
    val formattedBg = sb.reverse().toString()
    
    val prefix = if (isNegative) "-" else ""
    return prefix + formattedBg + dec
}

enum class NavigationTab {
    Accounts,
    Expenses,
    Incomes,
    Reports
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(viewModel: FinanceViewModel) {
    // Forcing Right-To-Left Layout direction for perfect Arabic orientation
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        var currentTab by remember { mutableStateOf(NavigationTab.Accounts) }

        val context = LocalContext.current
        val activity = context as? androidx.activity.ComponentActivity
        var showExitConfirmDialog by remember { mutableStateOf(false) }

        androidx.activity.compose.BackHandler(enabled = true) {
            showExitConfirmDialog = true
        }

        if (showExitConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showExitConfirmDialog = false },
                title = {
                    Text(
                        text = "تأكيد الخروج",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                text = {
                    Text(
                        text = "هل أنت متأكد من رغبتك في الخروج من التطبيق؟",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        onClick = {
                            showExitConfirmDialog = false
                            activity?.finish()
                        }
                    ) {
                        Text("خروج", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirmDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.Accounts,
                        onClick = { currentTab = NavigationTab.Accounts },
                        label = { Text("الحسابات والديون", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) },
                        icon = { Icon(Icons.Default.List, contentDescription = "الحسابات والديون") }
                    )
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.Expenses,
                        onClick = { currentTab = NavigationTab.Expenses },
                        label = { Text("المصروفات", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "المصروفات") }
                    )
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.Incomes,
                        onClick = { currentTab = NavigationTab.Incomes },
                        label = { Text("الإيرادات", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) },
                        icon = { Icon(Icons.Default.Star, contentDescription = "الإيرادات") }
                    )
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.Reports,
                        onClick = { currentTab = NavigationTab.Reports },
                        label = { Text("التقارير", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "التقارير") }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    NavigationTab.Accounts -> LedgerScreen(viewModel = viewModel)
                    NavigationTab.Expenses -> ExpensesScreen(viewModel = viewModel)
                    NavigationTab.Incomes -> IncomesScreen(viewModel = viewModel)
                    NavigationTab.Reports -> ReportsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(viewModel: FinanceViewModel) {
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedLedgerFilter by remember { mutableStateOf(0) } // 0 = الكل, 1 = مدينون, 2 = دائنون

    // Dialog state controllers
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAdjustBalanceDialog by remember { mutableStateOf<Pair<Account, Boolean>?>(null) } // Account -> isAddition
    var showEditAccountDialog by remember { mutableStateOf<Account?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Account?>(null) }

    // Filter accounts list based on tabs & search query
    val filteredAccounts = remember(accounts, searchQuery, selectedLedgerFilter) {
        accounts.filter { account ->
            val matchQuery = account.name.contains(searchQuery, ignoreCase = true)
            val matchFilter = when (selectedLedgerFilter) {
                1 -> account.balance > 0    // مدين (له مال)
                2 -> account.balance < 0    // دائن (عليه مال)
                else -> true                // الكل
            }
            matchQuery && matchFilter
        }
    }

    // Calculative values
    val totalIncome = accounts.sumOf { it.balance }

    Column(modifier = Modifier.fillMaxSize()) {
        // App bar top header exactly like the screenshot with Vibrant Palette styling
        TopAppBar(
            title = {
                if (isSearchActive) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث عن حساب...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_search_field")
                    )
                } else {
                    Text(
                        text = "الرئيسية",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
            actions = {
                IconButton(onClick = {
                    if (isSearchActive) {
                        searchQuery = ""
                    }
                    isSearchActive = !isSearchActive
                }) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = { showAddAccountDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "إضافة حساب",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        )

        // Custom tabs row aligned with Vibrant Palette theme guidelines
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf("الكل", "مدينون", "دائنون")
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedLedgerFilter == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { selectedLedgerFilter = index }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Styled mint active indicator line
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                            )
                        } else {
                            Box(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // List of custom items matching the screenshot
        Box(modifier = Modifier.weight(1f)) {
            if (filteredAccounts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = "لا توجد حسابات",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "لا توجد تفاصيل مطابقة",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "اضغط على زر '+' بالأسفل أو بالأعلى لإضافة عميل أو حساب مالي جديد",
                        fontFamily = FontFamily.SansSerif,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredAccounts, key = { it.id }) { account ->
                        AccountItemRow(
                            account = account,
                            onAddClick = { showAdjustBalanceDialog = Pair(account, true) },
                            onSubtractClick = { showAdjustBalanceDialog = Pair(account, false) },
                            onEditClick = { showEditAccountDialog = account },
                            onDeleteClick = { showDeleteConfirmDialog = account }
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.6f), thickness = 1.dp)
                    }
                }
            }

            // Small add customer floating action button aligned with Vibrant Palette theme Guidelines
            FloatingActionButton(
                onClick = { showAddAccountDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 72.dp, start = 16.dp)
                    .testTag("add_account_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "إضافة عميل أو حساب",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Bottom aggregate sums sticky panel with Vibrant Palette Colors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Balance sum
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = formatAmount(totalIncome),
                    color = when {
                        totalIncome > 0 -> Color(0xFF2E7D32)
                        totalIncome < 0 -> Color(0xFFC62828)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("total_sum_display")
                )
            }

            // Divider vertical
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )

            // Total label "اجمالي"
            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "اجمالي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // --- Dialog Implementations ---

    // Add Account Dialog
    if (showAddAccountDialog) {
        var nameText by remember { mutableStateOf("") }
        var phoneText by remember { mutableStateOf("") }
        var balanceText by remember { mutableStateOf("") }
        var isNameError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("إضافة حساب عميل جديد", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = {
                            nameText = it
                            isNameError = it.isBlank()
                        },
                        label = { Text("الاسم") },
                        isError = isNameError,
                        supportingText = { if (isNameError) Text("مطلوب اسم العميل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_account_name"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = formatInputAmount(it) },
                        label = { Text("الرصيد الأولي (اختياري، سالب للدائن)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_account_balance"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = { phoneText = it },
                        label = { Text("رقم الهاتف (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameText.isBlank()) {
                            isNameError = true
                        } else {
                            val parsedBalance = balanceText.replace(",", "").toDoubleOrNull() ?: 0.0
                            viewModel.addAccount(
                                name = nameText,
                                balance = parsedBalance,
                                phone = phoneText
                            )
                            showAddAccountDialog = false
                        }
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Plus OR Minus adjust balance dialog
    showAdjustBalanceDialog?.let { (account, isAddition) ->
        var numText by remember { mutableStateOf("") }
        var notesText by remember { mutableStateOf("") }
        var isNumError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAdjustBalanceDialog = null },
            title = {
                Text(
                    text = if (isAddition) "إضافة للحساب: ${account.name}" else "خصم من الحساب: ${account.name}",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Column {
                    Text("الرصيد الحالي: ${formatAmount(account.balance)}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = numText,
                        onValueChange = {
                            numText = formatInputAmount(it)
                            isNumError = it.replace(",", "").toDoubleOrNull() == null
                        },
                        label = { Text("المبلغ") },
                        isError = isNumError,
                        supportingText = { if (isNumError) Text("من فضلك أدخل رقماً صالحاً") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("adjust_amount_field"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("بيان أو ملاحظات (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedVal = numText.replace(",", "").toDoubleOrNull()
                        if (parsedVal == null) {
                            isNumError = true
                        } else {
                            // If isAddition, we + balance. If isSubtract, we - balance.
                            val factor = if (isAddition) parsedVal else -parsedVal
                            viewModel.updateAccountBalance(account, factor)
                            showAdjustBalanceDialog = null
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustBalanceDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Edit Account details dialog
    showEditAccountDialog?.let { account ->
        var nameText by remember { mutableStateOf(account.name) }
        var detailText by remember { mutableStateOf(account.detail) }
        var phoneText by remember { mutableStateOf(account.phone) }
        var isNameError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditAccountDialog = null },
            title = { Text("تعديل بيانات الحساب", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = {
                            nameText = it
                            isNameError = it.isBlank()
                        },
                        label = { Text("اسم الحساب") },
                        isError = isNameError,
                        supportingText = { if (isNameError) Text("الاسم مطلوب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneText,
                        onValueChange = { phoneText = it },
                        label = { Text("الرقم الهاتفي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = detailText,
                        onValueChange = { detailText = it },
                        label = { Text("ملاحظات إضافية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameText.isNotBlank()) {
                            viewModel.updateAccountDetails(account, nameText, detailText, phoneText)
                            showEditAccountDialog = null
                        } else {
                            isNameError = true
                        }
                    }
                ) {
                    Text("حفظ التغييرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAccountDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Confirm deletion check dialog
    showDeleteConfirmDialog?.let { account ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("تأكيد الحذف", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) },
            text = { Text("هل أنت متأكد من رغبتك في حذف الحساب المالي لعميلك '${account.name}' نهائياً بالكامل؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteAccount(account)
                        showDeleteConfirmDialog = null
                    }
                ) {
                    Text("حذف نهائي", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun AccountItemRow(
    account: Account,
    onAddClick: () -> Unit,
    onSubtractClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color.White)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Subtract Button (Red Pill matching the screenshot)
        Box(
            modifier = Modifier
                .width(54.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFC62828)) // Rich red
                    .clickable { onSubtractClick() },
                contentAlignment = Alignment.Center
            ) {
                // Short white line inside the red pill represents standard negative sign
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(2.5.dp)
                        .background(Color.White)
                )
            }
        }

        // Gray vertical divider line
        Box(
            modifier = Modifier
                .fillMaxHeight(0.7f)
                .width(1.dp)
                .background(Color.LightGray.copy(alpha = 0.8f))
        )

        // Add Button (Green Plus sign matching the screenshot)
        Box(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            // Bright green styled plus symbol
            Text(
                text = "+",
                color = Color(0xFF4CAF50),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }

        // Gray vertical divider line
        Box(
            modifier = Modifier
                .fillMaxHeight(0.7f)
                .width(1.dp)
                .background(Color.LightGray.copy(alpha = 0.8f))
        )

        // Middle element: The Account Balance Value
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatAmount(account.balance),
                color = when {
                    account.balance > 0 -> Color(0xFF2E7D32) // Custom green
                    account.balance < 0 -> Color(0xFFC62828) // Deep red
                    else -> Color.Black                      // Flat zero color
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right-middle: Account Name (Deep indigo blue text, bold)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 4.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = account.name,
                color = Color(0xFF0288D1), // Sky-blue bold shade from photo
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Far Right: Option drop menu
        Box(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = { expandedMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "المزيد",
                    tint = Color.Gray
                )
            }
            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("تعديل الاسم والتفاصيل") },
                    onClick = {
                        expandedMenu = false
                        onEditClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("حذف الحساب المالي") },
                    onClick = {
                        expandedMenu = false
                        onDeleteClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
            }
        }
    }
}


// --- EXPENSES SCREEN ---
// Explicitly requested: "عايز الشاشه المصروفات تكون فارقة (فارغة) و فيها زر صغير لاضافة مصروف جديد"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: FinanceViewModel) {
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    val totalExpensesSum = expenses.sumOf { it.amount }

    Column(modifier = Modifier.fillMaxSize()) {
        // App bar top header styled with Vibrant Palette colors
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "مصروفاتي",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    val arabicDateFormat = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar"))
                    Text(
                        text = arabicDateFormat.format(Date()),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
            actions = {
                IconButton(onClick = { showAddExpenseDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة مصروف", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        )

        // Expense metric dashboard banner in Vibrant theme colors
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "إجمالي المصروفات المسجلة",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatAmount(totalExpensesSum)} ج.س",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Section rendering or empty state
        Box(modifier = Modifier.weight(1f)) {
            if (expenses.isEmpty()) {
                // Renders the requested custom Empty State screen beautifully matching the Vibrant Palette
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Stylized wallet/notebook placeholder
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(Color(0xFFE6F3F3), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "دفتر فارغ",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "لا توجد مصروفات",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ابدأ بتسجيل أول مصروفاتك اليوم لتتبع ميزانيتك بشكل أفضل.",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Small elegant button to add new expense precisely inside the empty state
                    Button(
                        onClick = { showAddExpenseDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .widthIn(min = 180.dp)
                            .testTag("small_add_expense_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إضافة مصروف جديد",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(expenses) { expense ->
                        ExpenseCardRow(
                            expense = expense,
                            onDelete = { viewModel.deleteExpense(expense) },
                            onUpdateDetails = { newAmount, newDate, newNotes -> viewModel.updateExpenseDetails(expense, newAmount, newDate, newNotes) }
                        )
                    }
                }
            }
        }
    }

    // Add Expense Dialog Box
    if (showAddExpenseDialog) {
        var titleText by remember { mutableStateOf("") }
        var isTitleError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("تسجيل مصروف جديد", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = {
                            titleText = it
                            isTitleError = it.isBlank()
                        },
                        label = { Text("اسم المصروف") },
                        isError = isTitleError,
                        supportingText = { if (isTitleError) Text("الاسم مطلوب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_expense_title"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    onClick = {
                        if (titleText.isBlank()) {
                            isTitleError = true
                        } else {
                            viewModel.addExpense(
                                title = titleText,
                                amount = 0.0,
                                category = "عام",
                                notes = ""
                            )
                            showAddExpenseDialog = false
                        }
                    }
                ) {
                    Text("تسجيل", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

data class ExpenseTransaction(
    val amount: Double,
    val date: Long
)

fun serializeTransactions(transactions: List<ExpenseTransaction>): String {
    return transactions.joinToString("|") { "${it.amount},${it.date}" }
}

fun deserializeTransactions(str: String): List<ExpenseTransaction> {
    if (str.isBlank()) return emptyList()
    return str.split("|").mapNotNull {
        val parts = it.split(",")
        if (parts.size == 2) {
            val amount = parts[0].toDoubleOrNull()
            val date = parts[1].toLongOrNull()
            if (amount != null && date != null) {
                ExpenseTransaction(amount, date)
            } else null
        } else null
    }
}

fun getExpenseTransactions(expense: Expense): List<ExpenseTransaction> {
    val list = deserializeTransactions(expense.notes)
    if (list.isEmpty() && expense.amount > 0.0) {
        return listOf(ExpenseTransaction(expense.amount, expense.date))
    }
    return list
}

@Composable
fun ExpenseCardRow(expense: Expense, onDelete: () -> Unit, onUpdateDetails: (Double, Long, String) -> Unit) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val expenseDate = remember(expense.date) { formatter.format(Date(expense.date)) }
    var isExpanded by remember { mutableStateOf(false) }
    var showEditAmountDialog by remember { mutableStateOf(false) }

    val transactions = remember(expense.notes, expense.amount, expense.date) {
        getExpenseTransactions(expense)
    }
    val transactionsCount = transactions.size

    val todayTransactionsSum = remember(transactions) {
        val todayCal = Calendar.getInstance()
        val itemCal = Calendar.getInstance()
        transactions.filter { txn ->
            itemCal.timeInMillis = txn.date
            todayCal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
            todayCal.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)
        }.sumOf { it.amount }
    }

    var showDeleteExpenseConfirm by remember { mutableStateOf(false) }
    var showDeleteTxnConfirmIndex by remember { mutableStateOf<Int?>(null) }
    var showEditTxnDialog by remember { mutableStateOf<Pair<Int, ExpenseTransaction>?>(null) }

    if (showDeleteExpenseConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteExpenseConfirm = false },
            title = {
                Text(
                    text = "تأكيد حذف البند",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف بند المصروفات '${expense.title}' بالكامل بجميع عملياته؟ لا يمكن التراجع عن هذا الإجراء.",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDelete()
                        showDeleteExpenseConfirm = false
                    }
                ) {
                    Text("حذف نهائي", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteExpenseConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    showDeleteTxnConfirmIndex?.let { index ->
        val txn = transactions.getOrNull(index)
        if (txn != null) {
            AlertDialog(
                onDismissRequest = { showDeleteTxnConfirmIndex = null },
                title = {
                    Text(
                        text = "تأكيد حذف العملية",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                text = {
                    Text(
                        text = "هل أنت متأكد من رغبتك في حذف العملية المسجلة بمبلغ ${formatAmount(txn.amount)} ج.س؟",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            val currentTransactions = transactions.toMutableList()
                            currentTransactions.removeAt(index)
                            val newNotes = serializeTransactions(currentTransactions)
                            val newAmount = currentTransactions.sumOf { it.amount }
                            val newDate = currentTransactions.lastOrNull()?.date ?: System.currentTimeMillis()
                            onUpdateDetails(newAmount, newDate, newNotes)
                            showDeleteTxnConfirmIndex = null
                        }
                    ) {
                        Text("حذف", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteTxnConfirmIndex = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }

    showEditTxnDialog?.let { (index, txn) ->
        var inputVal by remember(txn) { mutableStateOf(formatInputAmount(txn.amount.toString())) }
        var inputError by remember { mutableStateOf(false) }
        var selectedDate by remember(txn) { mutableStateOf(txn.date) }

        AlertDialog(
            onDismissRequest = { showEditTxnDialog = null },
            title = {
                Text(
                    text = "تعديل قيم العملية",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "تعديل العملية رقم ${index + 1} للبند: ${expense.title}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputVal,
                            onValueChange = {
                                inputVal = formatInputAmount(it)
                                inputError = it.replace(",", "").toDoubleOrNull() == null
                            },
                            label = { Text("المبلغ (ج.س)") },
                            isError = inputError,
                            singleLine = true,
                            modifier = Modifier.weight(1.1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        // Date Picker trigger next to Amount field
                        val context = LocalContext.current
                        val dateDisplayFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                        var showDatePickerState by remember { mutableStateOf(false) }
                        
                        val calendar = remember { Calendar.getInstance().apply { timeInMillis = selectedDate } }
                        
                        if (showDatePickerState) {
                            val dateSetListener = android.app.DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                selectedDate = calendar.timeInMillis
                                showDatePickerState = false
                            }
                            
                            android.app.DatePickerDialog(
                                context,
                                dateSetListener,
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).apply {
                                setOnDismissListener { showDatePickerState = false }
                                show()
                            }
                        }

                        OutlinedTextField(
                            value = dateDisplayFormatter.format(Date(selectedDate)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("تاريخ العملية") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePickerState = true }) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "اختر التاريخ"
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .clickable { showDatePickerState = true },
                            singleLine = true
                        )
                    }
                    if (inputError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "مطلب إدخال رقم علمي أو صالح",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    onClick = {
                        val parsedAmt = inputVal.replace(",", "").toDoubleOrNull()
                        if (parsedAmt == null) {
                            inputError = true
                        } else {
                            val newTransactions = transactions.toMutableList()
                            newTransactions[index] = ExpenseTransaction(parsedAmt, selectedDate)
                            val newNotes = serializeTransactions(newTransactions)
                            val newAmount = newTransactions.sumOf { it.amount }
                            onUpdateDetails(newAmount, selectedDate, newNotes)
                            showEditTxnDialog = null
                        }
                    }
                ) {
                    Text("حفظ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTxnDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showEditAmountDialog) {
        var inputVal by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf(false) }
        var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

        AlertDialog(
            onDismissRequest = { showEditAmountDialog = false },
            title = {
                Text(
                    text = "إضافة عملية جديدة للمصروف",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "بند المصروف: ${expense.title}",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputVal,
                            onValueChange = {
                                inputVal = formatInputAmount(it)
                                inputError = it.replace(",", "").toDoubleOrNull() == null
                            },
                            label = { Text("المبلغ (ج.س)") },
                            isError = inputError,
                            singleLine = true,
                            modifier = Modifier.weight(1.1f).testTag("edit_expense_amount_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        // Date Picker trigger next to Amount field
                        val context = LocalContext.current
                        val dateDisplayFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                        var showDatePickerState by remember { mutableStateOf(false) }
                        
                        val calendar = remember { Calendar.getInstance().apply { timeInMillis = selectedDate } }
                        
                        if (showDatePickerState) {
                            val dateSetListener = android.app.DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                selectedDate = calendar.timeInMillis
                                showDatePickerState = false
                            }
                            
                            android.app.DatePickerDialog(
                                context,
                                dateSetListener,
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).apply {
                                setOnDismissListener { showDatePickerState = false }
                                show()
                            }
                        }

                        OutlinedTextField(
                            value = dateDisplayFormatter.format(Date(selectedDate)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("تاريخ العملية") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePickerState = true }) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "اختر التاريخ"
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .clickable { showDatePickerState = true },
                            singleLine = true
                        )
                    }
                    if (inputError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "مطلب إدخال رقم علمي أو صالح",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    onClick = {
                        val parsedAmt = inputVal.replace(",", "").toDoubleOrNull()
                        if (parsedAmt == null) {
                            inputError = true
                        } else {
                            val newTransactions = transactions.toMutableList()
                            newTransactions.add(ExpenseTransaction(parsedAmt, selectedDate))
                            val newNotes = serializeTransactions(newTransactions)
                            val newAmount = newTransactions.sumOf { it.amount }
                            onUpdateDetails(newAmount, selectedDate, newNotes)
                            showEditAmountDialog = false
                        }
                    }
                ) {
                    Text("إضافة", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAmountDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Trash icon to delete expense with confirmation check
                IconButton(onClick = { showDeleteExpenseConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف المصروف",
                        tint = Color.Red.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Cost tag formatted
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .width(110.dp)
                        .clickable { showEditAmountDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${formatAmount(todayTransactionsSum)} ج.س",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFFC62828), // Red tint signifies deduction cost
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "إضافة عملية",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    // Small Category pill tags
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = expense.category,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$transactionsCount عمليات",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Text titles block aligned Arabic side
                Column(
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expense.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "آخر تحديث: $expenseDate",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "العمليات المسجلة",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (transactions.isEmpty()) {
                        Text(
                            text = "لا توجد عمليات مسجلة لهذا البند. اضغط بالأسفل لإضافة عملية.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontFamily = FontFamily.SansSerif
                        )
                    } else {
                        transactions.forEachIndexed { index, txn ->
                            val txnDateStr = remember(txn.date) { formatter.format(Date(txn.date)) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Delete transaction under warning confirmation
                                    IconButton(
                                        onClick = {
                                            showDeleteTxnConfirmIndex = index
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف العملية",
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Edit transaction button
                                    IconButton(
                                        onClick = {
                                            showEditTxnDialog = index to txn
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل العملية",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Text(
                                        text = "${formatAmount(txn.amount)} ج.س",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828)
                                    )
                                }
                                Text(
                                    text = txnDateStr,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showEditAmountDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_expense_amount_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "إضافة عملية جديدة",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إضافة عملية جديدة لهذا البند",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// --- INCOMES SCREEN (الإيرادات) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomesScreen(viewModel: FinanceViewModel) {
    val incomes by viewModel.allIncomes.collectAsStateWithLifecycle()
    var showAddIncomeDialog by remember { mutableStateOf(false) }

    val totalIncomesSum = incomes.sumOf { it.amount }

    Column(modifier = Modifier.fillMaxSize()) {
        // App bar top header styled with Vibrant Palette colors
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "إيراداتي",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    val arabicDateFormat = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar"))
                    Text(
                        text = arabicDateFormat.format(Date()),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
            actions = {
                IconButton(onClick = { showAddIncomeDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة إيراد", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        )

        // Income metric dashboard banner in Vibrant theme colors with Green color tones
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), // Light Green container for Income
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "إجمالي الإيرادات المسجلة",
                        fontSize = 14.sp,
                        color = Color(0xFF1B5E20),
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatAmount(totalIncomesSum)} ج.س",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E7D32),
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF2E7D32).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32)
                    )
                }
            }
        }

        // Section rendering or empty state for Incomes
        Box(modifier = Modifier.weight(1f)) {
            if (incomes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Stylized wallet/notebook placeholder in Green theme
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(Color(0xFFE8F5E9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "دفتر فارغ",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "لا توجد إيرادات",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ابدأ بتسجيل أول إيراداتك اليوم لتتبع ميزانيتك بشكل أفضل.",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showAddIncomeDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF1B5E20)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .widthIn(min = 180.dp)
                            .testTag("small_add_income_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إضافة إيراد جديد",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(incomes) { income ->
                        IncomeCardRow(
                            income = income,
                            onDelete = { viewModel.deleteIncome(income) },
                            onUpdateDetails = { newAmount, newDate, newNotes -> viewModel.updateIncomeDetails(income, newAmount, newDate, newNotes) }
                        )
                    }
                }
            }
        }
    }

    // Add Income Dialog Box
    if (showAddIncomeDialog) {
        var titleText by remember { mutableStateOf("") }
        var isTitleError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddIncomeDialog = false },
            title = { Text("تسجيل إيراد جديد", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = {
                            titleText = it
                            isTitleError = it.isBlank()
                        },
                        label = { Text("اسم بند الإيراد") },
                        isError = isTitleError,
                        supportingText = { if (isTitleError) Text("الاسم مطلوب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_income_title"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    onClick = {
                        if (titleText.isBlank()) {
                            isTitleError = true
                        } else {
                            viewModel.addIncome(
                                title = titleText,
                                amount = 0.0,
                                category = "عام",
                                notes = ""
                            )
                            showAddIncomeDialog = false
                        }
                    }
                ) {
                    Text("تسجيل", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddIncomeDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

data class IncomeTransaction(
    val amount: Double,
    val date: Long
)

fun serializeIncomeTransactions(transactions: List<IncomeTransaction>): String {
    return transactions.joinToString("|") { "${it.amount},${it.date}" }
}

fun deserializeIncomeTransactions(str: String): List<IncomeTransaction> {
    if (str.isBlank()) return emptyList()
    return str.split("|").mapNotNull {
        val parts = it.split(",")
        if (parts.size == 2) {
            val amount = parts[0].toDoubleOrNull()
            val date = parts[1].toLongOrNull()
            if (amount != null && date != null) {
                IncomeTransaction(amount, date)
            } else null
        } else null
    }
}

fun getIncomeTransactions(income: Income): List<IncomeTransaction> {
    val list = deserializeIncomeTransactions(income.notes)
    if (list.isEmpty() && income.amount > 0.0) {
        return listOf(IncomeTransaction(income.amount, income.date))
    }
    return list
}

@Composable
fun IncomeCardRow(income: Income, onDelete: () -> Unit, onUpdateDetails: (Double, Long, String) -> Unit) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val incomeDate = remember(income.date) { formatter.format(Date(income.date)) }
    var isExpanded by remember { mutableStateOf(false) }
    var showEditAmountDialog by remember { mutableStateOf(false) }

    val transactions = remember(income.notes, income.amount, income.date) {
        getIncomeTransactions(income)
    }
    val transactionsCount = transactions.size

    val todayTransactionsSum = remember(transactions) {
        val todayCal = Calendar.getInstance()
        val itemCal = Calendar.getInstance()
        transactions.filter { txn ->
            itemCal.timeInMillis = txn.date
            todayCal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
            todayCal.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)
        }.sumOf { it.amount }
    }

    var showDeleteIncomeConfirm by remember { mutableStateOf(false) }
    var showDeleteTxnConfirmIndex by remember { mutableStateOf<Int?>(null) }
    var showEditTxnDialog by remember { mutableStateOf<Pair<Int, IncomeTransaction>?>(null) }

    if (showDeleteIncomeConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteIncomeConfirm = false },
            title = {
                Text(
                    text = "تأكيد حذف البند",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف بند الإيرادات '${income.title}' بالكامل بجميع عملياته؟ لا يمكن التراجع عن هذا الإجراء.",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDelete()
                        showDeleteIncomeConfirm = false
                    }
                ) {
                    Text("حذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteIncomeConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    showDeleteTxnConfirmIndex?.let { index ->
        val txn = transactions.getOrNull(index)
        if (txn != null) {
            AlertDialog(
                onDismissRequest = { showDeleteTxnConfirmIndex = null },
                title = {
                    Text(
                        text = "تأكيد حذف العملية",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                text = {
                    Text(
                        text = "هل أنت متأكد من رغبتك في حذف العملية المسجلة بمبلغ ${formatAmount(txn.amount)} ج.س؟",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            val currentTransactions = transactions.toMutableList()
                            currentTransactions.removeAt(index)
                            val newNotes = serializeIncomeTransactions(currentTransactions)
                            val newAmount = currentTransactions.sumOf { it.amount }
                            val newDate = currentTransactions.lastOrNull()?.date ?: System.currentTimeMillis()
                            onUpdateDetails(newAmount, newDate, newNotes)
                            showDeleteTxnConfirmIndex = null
                        }
                    ) {
                        Text("حذف", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteTxnConfirmIndex = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }

    showEditTxnDialog?.let { (index, txn) ->
        var inputVal by remember(txn) { mutableStateOf(formatInputAmount(txn.amount.toString())) }
        var inputError by remember { mutableStateOf(false) }
        var selectedDate by remember(txn) { mutableStateOf(txn.date) }

        AlertDialog(
            onDismissRequest = { showEditTxnDialog = null },
            title = {
                Text(
                    text = "تعديل قيم العملية",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "تعديل العملية رقم ${index + 1} للبند: ${income.title}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputVal,
                            onValueChange = {
                                inputVal = formatInputAmount(it)
                                inputError = it.replace(",", "").toDoubleOrNull() == null
                            },
                            label = { Text("المبلغ (ج.س)") },
                            isError = inputError,
                            singleLine = true,
                            modifier = Modifier.weight(1.1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        val context = LocalContext.current
                        val dateDisplayFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                        var showDatePickerState by remember { mutableStateOf(false) }
                        
                        val calendar = remember { Calendar.getInstance().apply { timeInMillis = selectedDate } }
                        
                        if (showDatePickerState) {
                            val dateSetListener = android.app.DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                selectedDate = calendar.timeInMillis
                                showDatePickerState = false
                            }
                            
                            android.app.DatePickerDialog(
                                context,
                                dateSetListener,
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).apply {
                                setOnDismissListener { showDatePickerState = false }
                                show()
                            }
                        }

                        OutlinedTextField(
                            value = dateDisplayFormatter.format(Date(selectedDate)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("تاريخ العملية") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePickerState = true }) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "اختر التاريخ"
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .clickable { showDatePickerState = true },
                            singleLine = true
                        )
                    }
                    if (inputError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "مطلب إدخال رقم علمي أو صالح",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    onClick = {
                        val parsedAmt = inputVal.replace(",", "").toDoubleOrNull()
                        if (parsedAmt == null) {
                            inputError = true
                        } else {
                            val newTransactions = transactions.toMutableList()
                            newTransactions[index] = IncomeTransaction(parsedAmt, selectedDate)
                            val newNotes = serializeIncomeTransactions(newTransactions)
                            val newAmount = newTransactions.sumOf { it.amount }
                            onUpdateDetails(newAmount, selectedDate, newNotes)
                            showEditTxnDialog = null
                        }
                    }
                ) {
                    Text("حفظ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTxnDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showEditAmountDialog) {
        var inputVal by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf(false) }
        var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

        AlertDialog(
            onDismissRequest = { showEditAmountDialog = false },
            title = {
                Text(
                    text = "إضافة عملية جديدة للإيراد",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "بند الإيراد: ${income.title}",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputVal,
                            onValueChange = {
                                inputVal = formatInputAmount(it)
                                inputError = it.replace(",", "").toDoubleOrNull() == null
                            },
                            label = { Text("المبلغ (ج.س)") },
                            isError = inputError,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        val context = LocalContext.current
                        val dateDisplayFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                        var showDatePickerState by remember { mutableStateOf(false) }
                        
                        val calendar = remember { Calendar.getInstance() }
                        
                        if (showDatePickerState) {
                            val dateSetListener = android.app.DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                selectedDate = calendar.timeInMillis
                                showDatePickerState = false
                            }
                            
                            android.app.DatePickerDialog(
                                context,
                                dateSetListener,
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).apply {
                                setOnDismissListener { showDatePickerState = false }
                                show()
                            }
                        }

                        OutlinedTextField(
                            value = dateDisplayFormatter.format(Date(selectedDate)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("التاريخ") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePickerState = true }) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "اختر التاريخ"
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .clickable { showDatePickerState = true },
                            singleLine = true
                        )
                    }
                    if (inputError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الرجاء إدخال مبلغ صحيح",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    onClick = {
                        val parsedAmt = inputVal.replace(",", "").toDoubleOrNull()
                        if (parsedAmt == null) {
                            inputError = true
                        } else {
                            val newTransactions = transactions.toMutableList()
                            newTransactions.add(IncomeTransaction(parsedAmt, selectedDate))
                            val newNotes = serializeIncomeTransactions(newTransactions)
                            val newAmount = newTransactions.sumOf { it.amount }
                            onUpdateDetails(newAmount, selectedDate, newNotes)
                            showEditAmountDialog = false
                        }
                    }
                ) {
                    Text("إضافة", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAmountDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showDeleteIncomeConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف الإيراد",
                        tint = Color.Red.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = income.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "آخر تحديث: $incomeDate",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .width(110.dp)
                        .clickable { showEditAmountDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${formatAmount(todayTransactionsSum)} ج.س",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFF2E7D32), // Green tint signifies added value
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "إضافة عملية",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = income.category,
                                fontSize = 9.sp,
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$transactionsCount عمليات",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "العمليات المسجلة",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (transactions.isEmpty()) {
                        Text(
                            text = "لا توجد عمليات مسجلة لهذا البند. اضغط بالأسفل لإضافة عملية.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontFamily = FontFamily.SansSerif
                        )
                    } else {
                        transactions.forEachIndexed { index, txn ->
                            val txnDateStr = remember(txn.date) { formatter.format(Date(txn.date)) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            showDeleteTxnConfirmIndex = index
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف العملية",
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            showEditTxnDialog = index to txn
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل العملية",
                                            tint = Color(0xFF2E7D32).copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Text(
                                        text = "${formatAmount(txn.amount)} ج.س",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                Text(
                                    text = txnDateStr,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showEditAmountDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF1B5E20)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_income_amount_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "إضافة عملية جديدة",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إضافة عملية جديدة لهذا البند",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// --- REPORTS SCREEN (التقارير) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: FinanceViewModel) {
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val incomes by viewModel.allIncomes.collectAsStateWithLifecycle()

    var isMultiDayMode by remember { mutableStateOf(false) } // false = تقرير يومي, true = تقرير فترة

    // Single day states
    var selectedSingleDay by remember { mutableStateOf(System.currentTimeMillis()) }

    // Multi day states
    var startDate by remember {
        mutableStateOf(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L) // Default last 7 days
    }
    var endDate by remember { mutableStateOf(System.currentTimeMillis()) }

    // Extract all transaction items from expenses and incomes
    val allTransactionItems = remember(expenses, incomes) {
        val list = mutableListOf<ReportTransactionItem>()
        expenses.forEach { exp ->
            getExpenseTransactions(exp).forEach { txn ->
                list.add(
                    ReportTransactionItem(
                        title = exp.title,
                        amount = txn.amount,
                        isIncome = false,
                        date = txn.date,
                        category = exp.category
                    )
                )
            }
        }
        incomes.forEach { inc ->
            getIncomeTransactions(inc).forEach { txn ->
                list.add(
                    ReportTransactionItem(
                        title = inc.title,
                        amount = txn.amount,
                        isIncome = true,
                        date = txn.date,
                        category = inc.category
                    )
                )
            }
        }
        list.sortedByDescending { it.date }
    }

    val context = LocalContext.current
    val displayFormatter = remember { SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")) }
    val dateDisplayFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Filter transactions for that specific day
    val dayTransactions = remember(allTransactionItems, selectedSingleDay) {
        allTransactionItems.filter { isSameDay(it.date, selectedSingleDay) }
    }
    val dayIncomes = remember(dayTransactions) { dayTransactions.filter { it.isIncome } }
    val dayExpenses = remember(dayTransactions) { dayTransactions.filter { !it.isIncome } }

    // Extract all filtered transactions inside range
    val periodTransactions = remember(allTransactionItems, startDate, endDate) {
        allTransactionItems.filter { isWithinRange(it.date, startDate, endDate) }
    }
    val periodIncomes = remember(periodTransactions) { groupReportItems(periodTransactions.filter { it.isIncome }) }
    val periodExpenses = remember(periodTransactions) { groupReportItems(periodTransactions.filter { !it.isIncome }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upper App Bar with Export Actions
        TopAppBar(
            title = {
                Text(
                    text = "تقارير الميزانية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            ),
            actions = {
                IconButton(
                    onClick = {
                        if (!isMultiDayMode) {
                            val formattedDate = displayFormatter.format(Date(selectedSingleDay))
                            exportDailyReport(context, formattedDate, dayIncomes, dayExpenses)
                        } else {
                            val formattedStart = dateDisplayFormatter.format(Date(startDate))
                            val formattedEnd = dateDisplayFormatter.format(Date(endDate))
                            exportPeriodReport(context, formattedStart, formattedEnd, periodIncomes, periodExpenses)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "تصدير التقرير ومشاركته",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        // Theme Switcher Tab Row: Daily vs Multi-Day
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isMultiDayMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isMultiDayMode = false }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "تقرير يوم محدد",
                    color = if (!isMultiDayMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isMultiDayMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { isMultiDayMode = true }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "تقرير لعدة أيام / فترة",
                    color = if (isMultiDayMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        if (!isMultiDayMode) {
            // SINGLE DAY REPORT
            val totalIncomes = dayTransactions.filter { it.isIncome }.sumOf { it.amount }
            val totalExpenses = dayTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val netProfit = totalIncomes - totalExpenses

            // Day Selector bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedSingleDay
                            add(Calendar.DAY_OF_YEAR, -1)
                        }
                        selectedSingleDay = cal.timeInMillis
                    }
                ) {
                    Text("◀ السابق", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val calendar = Calendar.getInstance().apply { timeInMillis = selectedSingleDay }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, day)
                                    selectedSingleDay = calendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "اختر تاريخًا",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayFormatter.format(Date(selectedSingleDay)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                TextButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedSingleDay
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        selectedSingleDay = cal.timeInMillis
                    }
                ) {
                    Text("التالي ▶", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
                }
            }

            // FILTER & METRICS FOR SINGLE DAY
            val dayIncomes = remember(dayTransactions) { dayTransactions.filter { it.isIncome } }
            val dayExpenses = remember(dayTransactions) { dayTransactions.filter { !it.isIncome } }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "تقرير مفصل ليوم " + SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(Date(selectedSingleDay)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                // Income Table for Single Day
                item {
                    ReportTable(
                        title = "جدول الإيرادات المسجلة",
                        headers = listOf("البند", "التاريخ والوقت", "المبلغ"),
                        items = dayIncomes,
                        isIncome = true,
                        emptyMessage = "لا توجد إيرادات مسجلة لليوم."
                    )
                }

                // Expense Table for Single Day
                item {
                    ReportTable(
                        title = "جدول المصروفات والبنود المسجلة",
                        headers = listOf("البند", "التاريخ والوقت", "المبلغ"),
                        items = dayExpenses,
                        isIncome = false,
                        emptyMessage = "لا توجد مصروفات مسجلة لليوم."
                    )
                }

                // Final Summary Panel at the End of the Report
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    FinalReportSummaryCard(
                        totalIncome = totalIncomes,
                        totalExpense = totalExpenses,
                        netProfit = netProfit,
                        titleText = "خلاصة تقرير اليوم"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val formattedDate = displayFormatter.format(Date(selectedSingleDay))
                            exportDailyReport(context, formattedDate, dayIncomes, dayExpenses)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_daily_report_btn")
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة وتصدير هذا التقرير"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مشاركة وتصدير هذا التقرير",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        } else {
            // DATE RANGE REPORT
            val context = LocalContext.current
            val dateDisplayFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

            // Period selector (two picker buttons side by side)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start date picker button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val calendar = Calendar.getInstance().apply { timeInMillis = startDate }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, day)
                                    startDate = calendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = "من تاريخ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.SansSerif
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = dateDisplayFormatter.format(Date(startDate)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // End date picker button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val calendar = Calendar.getInstance().apply { timeInMillis = endDate }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, day)
                                    endDate = calendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = "إلى تاريخ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.SansSerif
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = dateDisplayFormatter.format(Date(endDate)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Aggregate totals
            val totalIncomesPeriod = periodTransactions.filter { it.isIncome }.sumOf { it.amount }
            val totalExpensesPeriod = periodTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val netProfitPeriod = totalIncomesPeriod - totalExpensesPeriod

            // Group by day inside range
            val dayReports = remember(periodTransactions) {
                val grouped = periodTransactions.groupBy { item ->
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = item.date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    cal.timeInMillis
                }
                grouped.map { (dayStartTimestamp, items) ->
                    val dayIncomes = items.filter { it.isIncome }.sumOf { it.amount }
                    val dayExpenses = items.filter { !it.isIncome }.sumOf { it.amount }
                    val pFormatter = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar"))
                    DayReport(
                        dateString = pFormatter.format(Date(dayStartTimestamp)),
                        timestamp = dayStartTimestamp,
                        totalIncome = dayIncomes,
                        totalExpense = dayExpenses,
                        netProfit = dayIncomes - dayExpenses,
                        transactions = items.sortedBy { it.date }
                    )
                }.sortedByDescending { it.timestamp }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // General Report Title
                item {
                    val rangeFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale("ar")) }
                    Text(
                        text = "تقرير الفترة من ${rangeFormatter.format(Date(startDate))} إلى ${rangeFormatter.format(Date(endDate))}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                // Incomes Period Table
                item {
                    ReportTable(
                        title = "جدول إيرادات الفترة المحددة",
                        headers = listOf("البند", "التاريخ والوقت", "المبلغ"),
                        items = periodIncomes,
                        isIncome = true,
                        emptyMessage = "لا توجد إيرادات مسجلة في هذه الفترة."
                    )
                }

                // Expenses Period Table
                item {
                    ReportTable(
                        title = "جدول مصروفات الفترة المحددة",
                        headers = listOf("البند", "التاريخ والوقت", "المبلغ"),
                        items = periodExpenses,
                        isIncome = false,
                        emptyMessage = "لا توجد مصروفات مسجلة في هذه الفترة."
                    )
                }

                // General Period Report Summary Card
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    FinalReportSummaryCard(
                        totalIncome = totalIncomesPeriod,
                        totalExpense = totalExpensesPeriod,
                        netProfit = netProfitPeriod,
                        titleText = "خلاصة تقرير الفترة الإجمالية"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val formattedStart = dateDisplayFormatter.format(Date(startDate))
                            val formattedEnd = dateDisplayFormatter.format(Date(endDate))
                            exportPeriodReport(context, formattedStart, formattedEnd, periodIncomes, periodExpenses)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_period_report_btn")
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة وتصدير تقرير الفترة"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مشاركة وتصدير تقرير الفترة",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Header for Day by Day breakdown stacked in the same screen
                item {
                    Text(
                        text = "التقارير اليومية للفترة المسجلة (${dayReports.size} أيام)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                if (dayReports.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لا توجد أي عمليات في الفترة المحددة.",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(dayReports) { dayReport ->
                        DayReportCardRow(dayReport)
                    }
                }
            }
        }
    }
}

// Data models for reports
data class DayReport(
    val dateString: String,
    val timestamp: Long,
    val totalIncome: Double,
    val totalExpense: Double,
    val netProfit: Double,
    val transactions: List<ReportTransactionItem>
)

data class ReportTransactionItem(
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val date: Long,
    val category: String = "عام",
    val occurrenceCount: Int = 1
)

fun groupReportItems(items: List<ReportTransactionItem>): List<ReportTransactionItem> {
    return items.groupBy { it.title + "|||" + it.category }
        .map { (key, group) ->
            val parts = key.split("|||")
            val title = parts[0]
            val category = parts[1]
            val totalAmount = group.sumOf { it.amount }
            val latestDate = group.maxOfOrNull { it.date } ?: 0L
            val isIncome = group.first().isIncome
            ReportTransactionItem(
                title = title,
                amount = totalAmount,
                isIncome = isIncome,
                date = latestDate,
                category = category,
                occurrenceCount = group.size
            )
        }.sortedByDescending { it.amount } // Sort by amount descending to show main expenses first
}

// Helper to determine if dates are equal to precision of day
fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

// Helper to determine if date is within selected range inclusive of days
fun isWithinRange(time: Long, startTime: Long, endTime: Long): Boolean {
    val dateCal = Calendar.getInstance().apply { timeInMillis = time }
    val startCal = Calendar.getInstance().apply {
        timeInMillis = startTime
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val endCal = Calendar.getInstance().apply {
        timeInMillis = endTime
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return dateCal.timeInMillis in startCal.timeInMillis..endCal.timeInMillis
}

// Helpers for exporting reports and sharing them
fun shareReportText(context: android.content.Context, text: String, title: String) {
    try {
        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clipData = android.content.ClipData.newPlainText("تقرير مالي", text)
        clipboardManager.setPrimaryClip(clipData)
        android.widget.Toast.makeText(context, "تم نسخة التقرير للحافظة وجاري المشاركة...", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        // ignore clipboard error if any
    }
    
    val shareIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, title)
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة التقرير عبر:"))
}

fun exportDailyReport(context: android.content.Context, dateStr: String, incomes: List<ReportTransactionItem>, expenses: List<ReportTransactionItem>) {
    val totalIncomes = incomes.sumOf { it.amount }
    val totalExpenses = expenses.sumOf { it.amount }
    val netProfit = totalIncomes - totalExpenses
    
    val sb = java.lang.StringBuilder()
    sb.append("📊 **تقرير مالي يومي**\n")
    sb.append("التاريخ: $dateStr\n")
    sb.append("=========================\n\n")
    
    sb.append("📥 **الإيرادات / التوريدات:**\n")
    if (incomes.isEmpty()) {
        sb.append("- لا يوجد إيرادات مسجلة\n")
    } else {
        incomes.forEach { txn ->
            val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(txn.date))
            sb.append("- ${txn.title} (${txn.category}): ${formatAmount(txn.amount)} ج.س | الساعة: $time\n")
        }
    }
    sb.append("-------------------------\n")
    sb.append("إجمالي الإيرادات: ${formatAmount(totalIncomes)} ج.س\n\n")
    
    sb.append("📤 **المصروفات:**\n")
    if (expenses.isEmpty()) {
        sb.append("- لا يوجد مصروفات مسجلة\n")
    } else {
        expenses.forEach { txn ->
            val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(txn.date))
            sb.append("- ${txn.title} (${txn.category}): ${formatAmount(txn.amount)} ج.س | الساعة: $time\n")
        }
    }
    sb.append("-------------------------\n")
    sb.append("إجمالي المصروفات: ${formatAmount(totalExpenses)} ج.س\n\n")
    
    sb.append("=========================\n")
    sb.append("💵 **الخلاصة:**\n")
    sb.append("صافي الأرباح/الفائض: ${formatAmount(netProfit)} ج.س\n")
    sb.append("=========================\n")
    sb.append("تم تصديره بواسطة تطبيق إدارة المالية")
    
    shareReportText(context, sb.toString(), "تقرير مالي يومي - $dateStr")
}

fun exportPeriodReport(context: android.content.Context, startDateStr: String, endDateStr: String, incomes: List<ReportTransactionItem>, expenses: List<ReportTransactionItem>) {
    val totalIncomes = incomes.sumOf { it.amount }
    val totalExpenses = expenses.sumOf { it.amount }
    val netProfit = totalIncomes - totalExpenses
    
    val sb = java.lang.StringBuilder()
    sb.append("📊 **تقرير مالي للفترة**\n")
    sb.append("من تاريخ: $startDateStr إلى تاريخ: $endDateStr\n")
    sb.append("=========================\n\n")
    
    sb.append("📥 **الإيرادات / التوريدات:**\n")
    if (incomes.isEmpty()) {
        sb.append("- لا يوجد إيرادات مسجلة في هذه الفترة\n")
    } else {
        incomes.forEach { txn ->
            val dtStr = if (txn.occurrenceCount > 1) {
                "مكرر ${txn.occurrenceCount} مرات"
            } else {
                "تاريخ: " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(txn.date))
            }
            sb.append("- ${txn.title} (${txn.category}): ${formatAmount(txn.amount)} ج.س | $dtStr\n")
        }
    }
    sb.append("-------------------------\n")
    sb.append("إجمالي الإيرادات للفترة: ${formatAmount(totalIncomes)} ج.س\n\n")
    
    sb.append("📤 **المصروفات:**\n")
    if (expenses.isEmpty()) {
        sb.append("- لا يوجد مصروفات مسجلة في هذه الفترة\n")
    } else {
        expenses.forEach { txn ->
            val dtStr = if (txn.occurrenceCount > 1) {
                "مكرر ${txn.occurrenceCount} مرات"
            } else {
                "تاريخ: " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(txn.date))
            }
            sb.append("- ${txn.title} (${txn.category}): ${formatAmount(txn.amount)} ج.س | $dtStr\n")
        }
    }
    sb.append("-------------------------\n")
    sb.append("إجمالي المصروفات للفترة: ${formatAmount(totalExpenses)} ج.س\n\n")
    
    sb.append("=========================\n")
    sb.append("💵 **الخلاصة الإجمالية بالفترة:**\n")
    sb.append("صافي الأرباح/الفائض للفترة: ${formatAmount(netProfit)} ج.س\n")
    sb.append("=========================\n")
    sb.append("تم تصديره بواسطة تطبيق إدارة المالية")
    
    shareReportText(context, sb.toString(), "تقرير مالي للفترة")
}

// --- BEAUTIFUL MODERN TABLE COMPONENT ---
@Composable
fun ReportTable(
    title: String,
    headers: List<String>,
    items: List<ReportTransactionItem>,
    isIncome: Boolean,
    emptyMessage: String
) {
    val themeColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val headerBgColor = if (isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val totalAmount = items.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Table Title Header Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColor.copy(alpha = 0.08f))
                .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(themeColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.AddCircle else Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = themeColor,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // Table Column Headers Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBgColor)
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // column 1: البند
            Text(
                text = headers[0],
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = themeColor,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.Start
            )
            // column 2: التاريخ والوقت
            Text(
                text = headers[1],
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = themeColor,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1.4f),
                textAlign = TextAlign.Center
            )
            // column 3: المبلغ
            Text(
                text = headers[2],
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = themeColor,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1.1f),
                textAlign = TextAlign.End
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // Body rows of table
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val displayTimeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ar")) }
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item Band Title
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = item.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.category,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Date & Time Column
                    val dateFormatted = if (item.occurrenceCount > 1) {
                        "مكرر ${item.occurrenceCount} مرات"
                    } else {
                        displayTimeFormatter.format(Date(item.date))
                    }
                    Text(
                        text = dateFormatted,
                        fontSize = if (item.occurrenceCount > 1) 11.sp else 10.sp,
                        fontWeight = if (item.occurrenceCount > 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (item.occurrenceCount > 1) themeColor.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = if (item.occurrenceCount > 1) FontFamily.SansSerif else FontFamily.Monospace,
                        modifier = Modifier.weight(1.4f),
                        textAlign = TextAlign.Center
                    )

                    // Registered Amount
                    Text(
                        text = "${formatAmount(item.amount)} ج.س",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = themeColor,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1.1f),
                        textAlign = TextAlign.End
                    )
                }

                if (index < items.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // Table Footer Row (Total)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColor.copy(alpha = 0.05f))
                .padding(vertical = 11.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isIncome) "إجمالي الإيرادات" else "إجمالي المصروفات",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = themeColor,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(2.9f),
                textAlign = TextAlign.Start
            )

            Text(
                text = "${formatAmount(totalAmount)} ج.س",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = themeColor,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1.1f),
                textAlign = TextAlign.End
            )
        }
    }
}

// --- GRAND REPORT METRICS CARD COMPONENT ---
@Composable
fun FinalReportSummaryCard(
    totalIncome: Double,
    totalExpense: Double,
    netProfit: Double,
    titleText: String
) {
    val netProfitColor = if (netProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
    val netProfitBg = if (netProfit >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Summary Header Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = titleText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Registered Income Entry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إجمالي الإيرادات:",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "${formatAmount(totalIncome)} ج.س",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF2E7D32),
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Registered Expense Entry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إجمالي المصروفات:",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "${formatAmount(totalExpense)} ج.س",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFC62828),
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // Grand Net Expected Profit banner row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(netProfitBg)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "صافي الربح المتوقع",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = netProfitColor.copy(alpha = 0.85f),
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${formatAmount(netProfit)} ج.س",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = netProfitColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(netProfitColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (netProfit >= 0) Icons.Default.Star else Icons.Default.Warning,
                            contentDescription = null,
                            tint = netProfitColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayReportCardRow(dayReport: DayReport) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dayReport.dateString,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ايراد: ${formatAmount(dayReport.totalIncome)}",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "مصروف: ${formatAmount(dayReport.totalExpense)}",
                            fontSize = 11.sp,
                            color = Color(0xFFC62828),
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    val isProfit = dayReport.netProfit >= 0
                    val profitTextColor = if (isProfit) Color(0xFF2E7D32) else Color(0xFFC62828)
                    val profitBgColor = if (isProfit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(profitBgColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${formatAmount(dayReport.netProfit)} ج.س",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = profitTextColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${dayReport.transactions.size} عمليات (انقر للتفاصيل)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    val dayIncomes = remember(dayReport.transactions) { dayReport.transactions.filter { it.isIncome } }
                    val dayExpenses = remember(dayReport.transactions) { dayReport.transactions.filter { !it.isIncome } }

                    // Local Day Income Table
                    ReportTable(
                        title = "جدول إيرادات يوم " + dayReport.dateString,
                        headers = listOf("البند", "التاريخ والوقت", "المبلغ"),
                        items = dayIncomes,
                        isIncome = true,
                        emptyMessage = "لا توجد إيرادات مسجلة لليوم."
                    )

                    // Local Day Expense Table
                    ReportTable(
                        title = "جدول مصروفات يوم " + dayReport.dateString,
                        headers = listOf("البند", "التاريخ والوقت", "المبلغ"),
                        items = dayExpenses,
                        isIncome = false,
                        emptyMessage = "لا توجد مصروفات مسجلة لليوم."
                    )

                    // Expected Net Profit of that Day Card
                    FinalReportSummaryCard(
                        totalIncome = dayReport.totalIncome,
                        totalExpense = dayReport.totalExpense,
                        netProfit = dayReport.netProfit,
                        titleText = "خلاصة تقرير هذا اليوم"
                    )
                }
            }
        }
    }
}

@Composable
fun ReportTransactionRow(item: ReportTransactionItem) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(item.date) { formatter.format(Date(item.date)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (item.isIncome) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isIncome) Icons.Default.Add else Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = if (item.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = timeStr,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Text(
            text = "${if (item.isIncome) "+" else "-"}${formatAmount(item.amount)} ج.س",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = if (item.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
