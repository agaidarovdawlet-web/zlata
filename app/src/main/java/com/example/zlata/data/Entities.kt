package com.example.zlata.data

data class Customer(
    val id: Long,
    val lastName: String,
    val firstName: String,
    val middleName: String?,
    val phone: String?,
    val email: String?,
    val region: String
) {
    val fullName: String = listOf(lastName, firstName, middleName).filterNotNull().joinToString(" ")
}

data class Address(
    val id: Long,
    val city: String,
    val street: String,
    val house: String,
    val apartment: String?
)

data class PersonalAccount(
    val id: Long,
    val number: String,
    val customerId: Long,
    val customerName: String,
    val address: String,
    val serviceType: String,
    val balance: Double,
    val status: String
)

data class Meter(
    val id: Long,
    val accountId: Long,
    val accountNumber: String,
    val serialNumber: String,
    val type: String,
    val location: String,
    val nextVerificationDate: String,
    val lastReading: Double?
)

data class MeterReading(
    val id: Long,
    val meterId: Long,
    val readingDate: String,
    val value: Double,
    val method: String,
    val status: String
)

data class Payment(
    val id: Long,
    val accountNumber: String,
    val paymentDate: String,
    val amount: Double,
    val method: String,
    val status: String
)
