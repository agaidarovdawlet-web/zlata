package com.example.zlata.data

data class DashboardStats(
    val customerCount: Int = 0,
    val activeAccountCount: Int = 0,
    val meterCount: Int = 0,
    val debtTotal: Double = 0.0,
    val readingsThisMonth: Int = 0,
    val verificationsSoon: Int = 0
)

data class BusinessUiState(
    val stats: DashboardStats = DashboardStats(),
    val customers: List<Customer> = emptyList(),
    val accounts: List<PersonalAccount> = emptyList(),
    val meters: List<Meter> = emptyList(),
    val readings: List<MeterReading> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val query: String = "",
    val selectedMeterReadings: List<MeterReading> = emptyList()
)

data class SearchResult(
    val type: String,
    val title: String,
    val subtitle: String
)
