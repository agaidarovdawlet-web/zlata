package com.example.zlata.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.zlata.data.BusinessUiState
import com.example.zlata.data.Customer
import com.example.zlata.data.Meter
import com.example.zlata.data.MeterReading
import com.example.zlata.data.Payment
import com.example.zlata.data.PersonalAccount
import com.example.zlata.viewmodel.EnergoViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

private enum class Tab(val title: String, val icon: ImageVector) {
    Dashboard("Главная", Icons.Default.Home),
    Accounts("Счета", Icons.AutoMirrored.Filled.ReceiptLong),
    Meters("Счётчики", Icons.Default.Speed),
    Payments("Оплаты", Icons.Default.Payments),
    Customers("Клиенты", Icons.Default.People)
}

private enum class AccountFilter(val title: String) {
    All("Все"),
    Debt("Долг"),
    Active("Активные")
}

private enum class MeterFilter(val title: String) {
    All("Все"),
    Soon("Поверка скоро"),
    Expired("Просрочены")
}

@Composable
fun EnergoApp(viewModel: EnergoViewModel) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(Tab.Dashboard) }
    var readingMeter by remember { mutableStateOf<Meter?>(null) }
    var paymentAccount by remember { mutableStateOf<PersonalAccount?>(null) }

    Scaffold(
        topBar = { EnergoTopBar() },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.title) }
                    )
                }
            }
        },
        floatingActionButton = {
            when {
                selectedTab == Tab.Meters && state.meters.isNotEmpty() -> {
                    FloatingActionButton(onClick = { readingMeter = state.meters.first() }) {
                        Icon(Icons.Default.Add, contentDescription = "Внести показание")
                    }
                }
                selectedTab == Tab.Accounts && state.accounts.any { it.balance < 0 } -> {
                    FloatingActionButton(onClick = { paymentAccount = state.accounts.first { it.balance < 0 } }) {
                        Icon(Icons.Default.Payments, contentDescription = "Оплатить")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            SearchField(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            when (selectedTab) {
                Tab.Dashboard -> DashboardScreen(
                    state = state,
                    onMeterClick = {
                        viewModel.loadMeterReadings(it)
                        selectedTab = Tab.Meters
                    },
                    onAddReading = { readingMeter = it }
                )
                Tab.Accounts -> AccountsScreen(state.accounts, onPay = { paymentAccount = it })
                Tab.Meters -> MetersScreen(
                    meters = state.meters,
                    readings = state.selectedMeterReadings,
                    onMeterClick = viewModel::loadMeterReadings,
                    onAddReading = { readingMeter = it }
                )
                Tab.Payments -> PaymentsScreen(state.payments)
                Tab.Customers -> CustomersScreen(state.customers)
            }
        }
    }

    readingMeter?.let { meter ->
        ReadingDialog(
            meter = meter,
            onDismiss = { readingMeter = null },
            onSave = { date, value, method ->
                viewModel.addReading(meter.id, date, value, method)
                readingMeter = null
            }
        )
    }

    paymentAccount?.let { account ->
        PaymentDialog(
            account = account,
            onDismiss = { paymentAccount = null },
            onSave = { date, amount, method ->
                viewModel.addPayment(account.id, date, amount, method)
                paymentAccount = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnergoTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("T+", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("ЭнергосбыТ Плюс", fontWeight = FontWeight.SemiBold)
                    Text("Лицевые счета и показания", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        label = { Text("ФИО, лицевой счёт, адрес или счётчик") }
    )
}

@Composable
private fun DashboardScreen(state: BusinessUiState, onMeterClick: (Long) -> Unit, onAddReading: (Meter) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Клиенты", state.stats.customerCount.toString(), Icons.Default.People, Modifier.weight(1f))
                StatCard("Активные счета", state.stats.activeAccountCount.toString(), Icons.AutoMirrored.Filled.ReceiptLong, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Счётчики", state.stats.meterCount.toString(), Icons.Default.Speed, Modifier.weight(1f))
                StatCard("Долг", money(kotlin.math.abs(state.stats.debtTotal)), Icons.Default.Payments, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Показания за месяц", state.stats.readingsThisMonth.toString(), Icons.Default.Verified, Modifier.weight(1f))
                StatCard("Поверка скоро", state.stats.verificationsSoon.toString(), Icons.Default.Speed, Modifier.weight(1f))
            }
        }
        item { SectionTitle("Ближайшие поверки") }
        items(state.meters.take(5), key = { "dash-meter-${it.id}" }) { meter ->
            MeterRow(
                meter = meter,
                onClick = { onMeterClick(meter.id) },
                onAddReading = { onAddReading(meter) }
            )
            HorizontalDivider()
        }
        item { SectionTitle("Последние оплаты") }
        items(state.payments.take(4), key = { "dash-payment-${it.id}" }) { payment ->
            PaymentRow(payment)
            HorizontalDivider()
        }
    }
}

@Composable
private fun AccountsScreen(accounts: List<PersonalAccount>, onPay: (PersonalAccount) -> Unit) {
    var filter by remember { mutableStateOf(AccountFilter.All) }
    val filteredAccounts = remember(accounts, filter) {
        when (filter) {
            AccountFilter.All -> accounts
            AccountFilter.Debt -> accounts.filter { it.balance < 0 }
            AccountFilter.Active -> accounts.filter { it.status == "Активен" }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            FilterRow(
                selected = filter,
                values = AccountFilter.entries,
                title = { it.title },
                onSelected = { filter = it }
            )
        }
        if (filteredAccounts.isEmpty()) item { EmptyText("Лицевые счета не найдены") }
        items(filteredAccounts, key = { "account-${it.id}" }) { account ->
            AccountRow(account = account, onPay = { onPay(account) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun MetersScreen(
    meters: List<Meter>,
    readings: List<MeterReading>,
    onMeterClick: (Long) -> Unit,
    onAddReading: (Meter) -> Unit
) {
    var filter by remember { mutableStateOf(MeterFilter.All) }
    val filteredMeters = remember(meters, filter) {
        when (filter) {
            MeterFilter.All -> meters
            MeterFilter.Soon -> meters.filter { verificationDays(it.nextVerificationDate) in 0..30 }
            MeterFilter.Expired -> meters.filter { verificationDays(it.nextVerificationDate) < 0 }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            FilterRow(
                selected = filter,
                values = MeterFilter.entries,
                title = { it.title },
                onSelected = { filter = it }
            )
        }
        if (filteredMeters.isEmpty()) item { EmptyText("Счётчики не найдены") }
        items(filteredMeters, key = { "meter-${it.id}" }) { meter ->
            MeterRow(meter = meter, onClick = { onMeterClick(meter.id) }, onAddReading = { onAddReading(meter) })
            HorizontalDivider()
        }
        if (readings.isNotEmpty()) {
            item {
                Spacer(Modifier.height(10.dp))
                SectionTitle("История выбранного счётчика")
            }
            items(readings, key = { "reading-${it.id}" }) { reading ->
                ReadingRow(reading)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun PaymentsScreen(payments: List<Payment>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (payments.isEmpty()) item { EmptyText("Оплаты не найдены") }
        items(payments, key = { "payment-${it.id}" }) { payment ->
            PaymentRow(payment)
            HorizontalDivider()
        }
    }
}

@Composable
private fun CustomersScreen(customers: List<Customer>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (customers.isEmpty()) item { EmptyText("Клиенты не найдены") }
        items(customers, key = { "customer-${it.id}" }) { customer ->
            ListItem(
                headlineContent = { Text(customer.fullName, fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text(listOfNotNull(customer.region, customer.phone, customer.email).joinToString(" • ")) },
                leadingContent = { Icon(Icons.Default.People, contentDescription = null) },
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun AccountRow(account: PersonalAccount, onPay: () -> Unit) {
    val balanceColor = if (account.balance < 0) Color(0xFFC62828) else Color(0xFF2E7D32)
    ListItem(
        headlineContent = { Text("№ ${account.number}", fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text("${account.customerName} • ${account.address}\n${account.serviceType} • ${account.status}") },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(money(account.balance), color = balanceColor, fontWeight = FontWeight.SemiBold)
                if (account.balance < 0) {
                    TextButton(onClick = onPay) { Text("Оплатить") }
                }
            }
        },
        leadingContent = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) },
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    )
}

@Composable
private fun MeterRow(meter: Meter, onClick: () -> Unit, onAddReading: () -> Unit) {
    val statusText = verificationText(meter.nextVerificationDate)
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        ListItem(
            headlineContent = { Text("${meter.type} ${meter.serialNumber}", fontWeight = FontWeight.SemiBold) },
            supportingContent = {
                Text("Л/с ${meter.accountNumber} • ${meter.location}\n$statusText • последнее: ${meter.lastReading ?: "-"}")
            },
            leadingContent = { VerificationDot(meter.nextVerificationDate) }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onClick) {
                Text("История")
            }
            TextButton(onClick = onAddReading) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Внести")
            }
        }
    }
}

@Composable
private fun PaymentDialog(account: PersonalAccount, onDismiss: () -> Unit, onSave: (String, Double, String) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var amount by remember { mutableStateOf(kotlin.math.abs(account.balance).takeIf { it > 0 }?.toString() ?: "") }
    var method by remember { mutableStateOf("СБП") }
    val parsedAmount = amount.replace(',', '.').toDoubleOrNull()
    val methods = listOf("СБП", "Банковская карта", "Касса")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Оплата лицевого счёта") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("№ ${account.number}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(account.customerName, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Дата") }, singleLine = true)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' || char == ',' } },
                    label = { Text("Сумма") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { item ->
                        AssistChip(onClick = { method = item }, label = { Text(item) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsedAmount != null && parsedAmount > 0 && date.length == 10,
                onClick = { onSave(date, parsedAmount ?: 0.0, method) }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun ReadingRow(reading: MeterReading) {
    ListItem(
        headlineContent = { Text(reading.value.toString(), fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text("${reading.readingDate} • ${reading.method}") },
        trailingContent = { Text(reading.status, style = MaterialTheme.typography.bodySmall) }
    )
}

@Composable
private fun PaymentRow(payment: Payment) {
    ListItem(
        headlineContent = { Text(money(payment.amount), fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text("Л/с ${payment.accountNumber} • ${payment.paymentDate} • ${payment.method}") },
        trailingContent = { Text(payment.status, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(Icons.Default.Payments, contentDescription = null) },
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    )
}

@Composable
private fun ReadingDialog(meter: Meter, onDismiss: () -> Unit, onSave: (String, Double, String) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var value by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Мобильное приложение") }
    val parsedValue = value.replace(',', '.').toDoubleOrNull()
    val methods = listOf("Мобильное приложение", "Лично", "По телефону")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Внести показание") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${meter.type} • ${meter.serialNumber}", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Дата") }, singleLine = true)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { char -> char.isDigit() || char == '.' || char == ',' } },
                    label = { Text("Показание") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { item ->
                        AssistChip(onClick = { method = item }, label = { Text(item) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsedValue != null && parsedValue >= 0 && date.length == 10,
                onClick = { onSave(date, parsedValue ?: 0.0, method) }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun VerificationDot(date: String) {
    val days = verificationDays(date)
    val color = when {
        days < 0 -> Color(0xFFC62828)
        days <= 30 -> Color(0xFFF57C00)
        else -> Color(0xFF2E7D32)
    }
    Spacer(
        Modifier
            .width(12.dp)
            .height(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun <T> FilterRow(selected: T, values: List<T>, title: (T) -> String, onSelected: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        values.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(title(value)) }
            )
        }
    }
}

private fun verificationDays(date: String): Long =
    runCatching { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(date)) }.getOrDefault(-1)

private fun verificationText(date: String): String {
    val days = verificationDays(date)
    return when {
        days < 0 -> "Поверка просрочена: $date"
        days == 0L -> "Поверка сегодня"
        days <= 30 -> "Поверка через $days дн."
        else -> "Поверка: $date"
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun money(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU")).format(value)
