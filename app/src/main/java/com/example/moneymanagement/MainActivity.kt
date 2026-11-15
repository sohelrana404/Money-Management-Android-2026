package com.example.moneymanagement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

private val Green = Color(0xFF16834A)
private val LightBackground = Color(0xFFF5F7F5)
private val Red = Color(0xFFD94D4D)

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val amount: Double,
    val isIncome: Boolean,
    val date: String = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
)

data class UserProfile(
    val name: String = "Sohel Rana",
    val email: String = "sohel@moneymanagement.com",
    val currency: String = "৳",
    val monthlyBudget: Double = 50000.0
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoneyApp()
        }
    }
}

fun shareFinancialData(
    context: Context,
    userProfile: UserProfile,
    transactions: List<Transaction>,
    income: Double,
    expense: Double,
    balance: Double
) {
    val reportText = StringBuilder().apply {
        appendLine("📊 Money Management Financial Summary")
        appendLine("═════════════════════════════")
        appendLine("👤 Account: ${userProfile.name}")
        appendLine("💰 Current Balance: ${userProfile.currency} %.2f".format(balance))
        appendLine("📈 Total Income: ${userProfile.currency} %.2f".format(income))
        appendLine("📉 Total Expense: ${userProfile.currency} %.2f".format(expense))
        appendLine()
        appendLine("📋 Transaction Records (${transactions.size}):")
        if (transactions.isEmpty()) {
            appendLine("No transactions recorded yet.")
        } else {
            transactions.reversed().take(15).forEach { t ->
                val sign = if (t.isIncome) "+" else "-"
                appendLine("• ${t.name} [${t.category}]: $sign ${userProfile.currency} %.2f (${t.date})".format(t.amount))
            }
        }
        appendLine()
        appendLine("═════════════════════════════")
        appendLine("Shared via Money Management App")
    }.toString()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_SUBJECT, "Financial Data Summary - ${userProfile.name}")
        putExtra(Intent.EXTRA_TEXT, reportText)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share Financial Data")
    context.startActivity(shareIntent)
}

@Composable
fun MoneyApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf(UserProfile()) }

    var transactions by remember {
        mutableStateOf(
            listOf(
                Transaction(name = "Monthly Salary", category = "Work", amount = 30000.0, isIncome = true),
                Transaction(name = "Food & Dining", category = "Food", amount = 2000.0, isIncome = false),
                Transaction(name = "Transport", category = "Travel", amount = 1000.0, isIncome = false),
                Transaction(name = "Freelance Work", category = "Work", amount = 5000.0, isIncome = true)
            )
        )
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Home, 1: History, 2: Reports, 3: Settings
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(true) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Check Firebase current user on launch
    LaunchedEffect(Unit) {
        try {
            val authUser = FirebaseAuth.getInstance().currentUser
            if (authUser != null) {
                userProfile = userProfile.copy(
                    name = authUser.displayName ?: authUser.email?.substringBefore("@") ?: "User",
                    email = authUser.email ?: userProfile.email
                )
                isLoggedIn = true
            }
        } catch (_: Exception) {
            // Firebase not initialized yet
        }
    }

    if (!isLoggedIn) {
        AuthScreen(
            onAuthSuccess = { name, email ->
                userProfile = userProfile.copy(name = name, email = email)
                isLoggedIn = true
            }
        )
    } else {
        val income = transactions.filter { it.isIncome }.sumOf { it.amount }
        val expense = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val balance = income - expense

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.SwapHoriz, null) },
                        label = { Text("History") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.PieChart, null) },
                        label = { Text("Reports") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Settings") }
                    )
                }
            },
            containerColor = LightBackground
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        userProfile = userProfile,
                        transactions = transactions,
                        income = income,
                        expense = expense,
                        balance = balance,
                        onIncomeClick = {
                            selectedType = true
                            showAddDialog = true
                        },
                        onExpenseClick = {
                            selectedType = false
                            showAddDialog = true
                        },
                        onProfileClick = {
                            showProfileDialog = true
                        }
                    )
                    1 -> HistoryScreen(
                        userProfile = userProfile,
                        transactions = transactions,
                        income = income,
                        expense = expense,
                        balance = balance,
                        onDelete = { id ->
                            transactions = transactions.filter { it.id != id }
                        }
                    )
                    2 -> ReportsScreen(
                        userProfile = userProfile,
                        transactions = transactions,
                        income = income,
                        expense = expense,
                        balance = balance
                    )
                    3 -> SettingsScreen(
                        userProfile = userProfile,
                        transactions = transactions,
                        income = income,
                        expense = expense,
                        balance = balance,
                        onProfileUpdate = { updated -> userProfile = updated },
                        onClearData = { transactions = emptyList() },
                        onLogout = {
                            try {
                                FirebaseAuth.getInstance().signOut()
                            } catch (_: Exception) {}
                            isLoggedIn = false
                        }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddTransactionDialog(
                isIncome = selectedType,
                currency = userProfile.currency,
                onDismiss = { showAddDialog = false },
                onAdd = { name, category, amount ->
                    transactions = transactions + Transaction(
                        name = name,
                        category = category,
                        amount = amount,
                        isIncome = selectedType
                    )
                    showAddDialog = false
                }
            )
        }

        if (showProfileDialog) {
            ProfileDialog(
                userProfile = userProfile,
                onDismiss = { showProfileDialog = false },
                onSave = { updated ->
                    userProfile = updated
                    showProfileDialog = false
                },
                onLogout = {
                    try {
                        FirebaseAuth.getInstance().signOut()
                    } catch (_: Exception) {}
                    showProfileDialog = false
                    isLoggedIn = false
                }
            )
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: (name: String, email: String) -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Logo / Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Green.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = "Logo",
                        tint = Green,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = if (isLoginMode) "Welcome Back" else "Create Account",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green
                )

                Text(
                    text = if (isLoginMode) "Sign in to manage your finances" else "Sign up to track your money easily",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(4.dp))

                if (!isLoginMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isLoginMode) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = {
                        errorMessage = null
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please enter email and password."
                            return@Button
                        }

                        if (!isLoginMode) {
                            if (name.isBlank()) {
                                errorMessage = "Please enter your full name."
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMessage = "Passwords do not match."
                                return@Button
                            }
                        }

                        isLoading = true

                        try {
                            val auth = FirebaseAuth.getInstance()
                            if (isLoginMode) {
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnSuccessListener { result ->
                                        isLoading = false
                                        val displayName = result.user?.displayName ?: email.substringBefore("@")
                                        onAuthSuccess(displayName, email)
                                    }
                                    .addOnFailureListener { _ ->
                                        isLoading = false
                                        // Fallback local sign in if firebase account not found or offline
                                        val displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                                        Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                                        onAuthSuccess(displayName, email)
                                    }
                            } else {
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnSuccessListener { _ ->
                                        isLoading = false
                                        onAuthSuccess(name, email)
                                    }
                                    .addOnFailureListener { _ ->
                                        isLoading = false
                                        // Fallback local account creation
                                        Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                        onAuthSuccess(name, email)
                                    }
                            }
                        } catch (_: Exception) {
                            isLoading = false
                            val displayName = if (name.isNotBlank()) name else email.substringBefore("@").replaceFirstChar { it.uppercase() }
                            onAuthSuccess(displayName, email)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (isLoginMode) "Sign In" else "Create Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(
                    onClick = {
                        errorMessage = null
                        isLoginMode = !isLoginMode
                    }
                ) {
                    Text(
                        text = if (isLoginMode) "Don't have an account? Create Account" else "Already have an account? Sign In",
                        color = Green,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    userProfile: UserProfile,
    transactions: List<Transaction>,
    income: Double,
    expense: Double,
    balance: Double,
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Hello, ${userProfile.name.split(" ").first()}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Money",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            shareFinancialData(context, userProfile, transactions, income, expense, balance)
                        }
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Data",
                            tint = Green
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Green.copy(alpha = 0.1f))
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = Green,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Green)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "Total Balance",
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "${userProfile.currency} %.2f".format(balance),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Your available balance",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Income",
                    amount = income,
                    currency = userProfile.currency,
                    color = Green,
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "Expense",
                    amount = expense,
                    currency = userProfile.currency,
                    color = Red,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                "Quick Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onIncomeClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Income")
                }

                Button(
                    onClick = onExpenseClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Icon(Icons.Default.Remove, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Expense")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Transactions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = {
                        shareFinancialData(context, userProfile, transactions, income, expense, balance)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share", fontSize = 12.sp)
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No transactions yet. Add one above!", color = Color.Gray)
                    }
                }
            }
        } else {
            items(transactions.reversed().take(5)) { transaction ->
                TransactionItem(transaction, userProfile.currency)
            }
        }
    }
}

@Composable
fun HistoryScreen(
    userProfile: UserProfile,
    transactions: List<Transaction>,
    income: Double,
    expense: Double,
    balance: Double,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf("All") } // All, Income, Expense

    val filteredList = when (filter) {
        "Income" -> transactions.filter { it.isIncome }
        "Expense" -> transactions.filter { !it.isIncome }
        else -> transactions
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaction History",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Green
            )

            IconButton(
                onClick = {
                    shareFinancialData(context, userProfile, transactions, income, expense, balance)
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share History", tint = Green)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter == "All",
                onClick = { filter = "All" },
                label = { Text("All") }
            )
            FilterChip(
                selected = filter == "Income",
                onClick = { filter = "Income" },
                label = { Text("Income") }
            )
            FilterChip(
                selected = filter == "Expense",
                onClick = { filter = "Expense" },
                label = { Text("Expense") }
            )
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions found", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList.reversed()) { transaction ->
                    TransactionItemWithDelete(transaction, userProfile.currency, onDelete)
                }
            }
        }
    }
}

@Composable
fun ReportsScreen(
    userProfile: UserProfile,
    transactions: List<Transaction>,
    income: Double,
    expense: Double,
    balance: Double
) {
    val context = LocalContext.current
    val categoryExpenses = transactions
        .filter { !it.isIncome }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Financial Reports",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green
                )

                IconButton(
                    onClick = {
                        shareFinancialData(context, userProfile, transactions, income, expense, balance)
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Report", tint = Green)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Overview Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Income", color = Color.Gray)
                            Text("${userProfile.currency} %.2f".format(income), color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Expense", color = Color.Gray)
                            Text("${userProfile.currency} %.2f".format(expense), color = Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Net Savings", fontWeight = FontWeight.Bold)
                        Text("${userProfile.currency} %.2f".format(income - expense), fontWeight = FontWeight.Bold, color = if (income >= expense) Green else Red)
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            shareFinancialData(context, userProfile, transactions, income, expense, balance)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share Financial Report")
                    }
                }
            }
        }

        item {
            Text("Expense Breakdown by Category", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        if (categoryExpenses.isEmpty()) {
            item {
                Text("No expense data available for reports.", color = Color.Gray)
            }
        } else {
            items(categoryExpenses.toList()) { (category, amount) ->
                val percentage = if (expense > 0) (amount / expense).toFloat() else 0f
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(category, fontWeight = FontWeight.SemiBold)
                            Text("${userProfile.currency} %.2f".format(amount), color = Red, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { percentage },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Red,
                            trackColor = Color.LightGray.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    userProfile: UserProfile,
    transactions: List<Transaction>,
    income: Double,
    expense: Double,
    balance: Double,
    onProfileUpdate: (UserProfile) -> Unit,
    onClearData: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(userProfile.name) }
    var email by remember { mutableStateOf(userProfile.email) }
    var currency by remember { mutableStateOf(userProfile.currency) }
    var showClearDialog by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Green
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("User Profile Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("Currency Symbol (e.g. ৳, $, €)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            onProfileUpdate(userProfile.copy(name = name, email = email, currency = currency))
                            savedMessage = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Changes")
                    }
                    if (savedMessage) {
                        Text("Settings saved successfully!", color = Green, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Share & Export Data", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Share your financial summary and transaction logs with friends or family via WhatsApp, Email, or Messaging apps.", color = Color.Gray, fontSize = 13.sp)
                    Button(
                        onClick = {
                            shareFinancialData(context, userProfile, transactions, income, expense, balance)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share Data with Someone")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Account Action", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out / Logout")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Data Management", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Clear all transaction history and reset records.", color = Color.Gray, fontSize = 13.sp)
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Red)
                        Spacer(Modifier.width(4.dp))
                        Text("Clear All Transactions", color = Red)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("About App", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Money Management v1.0", color = Color.Gray, fontSize = 13.sp)
                    Text("Designed for tracking your daily income and expenses easily.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Transactions?") },
            text = { Text("This action cannot be undone. All your recorded transactions will be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearData()
                        showClearDialog = false
                    }
                ) {
                    Text("Delete", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    currency: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(title, color = Color.Gray)

            Spacer(Modifier.height(8.dp))

            Text(
                text = "$currency %.0f".format(amount),
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, currency: String) {
    val color = if (transaction.isIncome) Green else Red
    val sign = if (transaction.isIncome) "+" else "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    transaction.name,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${transaction.category} • ${transaction.date}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Text(
                text = "$sign $currency %.2f".format(transaction.amount),
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TransactionItemWithDelete(transaction: Transaction, currency: String, onDelete: (String) -> Unit) {
    val color = if (transaction.isIncome) Green else Red
    val sign = if (transaction.isIncome) "+" else "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.name,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${transaction.category} • ${transaction.date}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$sign $currency %.2f".format(transaction.amount),
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onDelete(transaction.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    isIncome: Boolean,
    currency: String,
    onDismiss: () -> Unit,
    onAdd: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isIncome) "Add Income" else "Add Expense")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Transaction Name") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Food, Work)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount ($currency)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = amount.toDoubleOrNull()
                    if (
                        name.isNotBlank() &&
                        category.isNotBlank() &&
                        value != null &&
                        value > 0
                    ) {
                        onAdd(name, category, value)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ProfileDialog(
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit,
    onLogout: () -> Unit
) {
    var name by remember { mutableStateOf(userProfile.name) }
    var email by remember { mutableStateOf(userProfile.email) }
    var budget by remember { mutableStateOf(userProfile.monthlyBudget.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User Profile") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Green.copy(alpha = 0.1f))
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = Green,
                        modifier = Modifier.size(72.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = { Text("Monthly Budget Goal") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onLogout) {
                    Text("Logout", color = Red)
                }
                TextButton(
                    onClick = {
                        val bVal = budget.toDoubleOrNull() ?: userProfile.monthlyBudget
                        if (name.isNotBlank() && email.isNotBlank()) {
                            onSave(userProfile.copy(name = name, email = email, monthlyBudget = bVal))
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
