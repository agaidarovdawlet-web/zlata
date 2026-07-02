package com.example.zlata.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EnergoRepository(private val database: EnergoDatabase) {
    suspend fun loadState(query: String = ""): BusinessUiState = withContext(Dispatchers.IO) {
        val db = database.readableDatabase()
        try {
            BusinessUiState(
                stats = DashboardStats(
                    customerCount = db.scalarInt("SELECT COUNT(*) FROM customers"),
                    activeAccountCount = db.scalarInt("SELECT COUNT(*) FROM personal_accounts WHERE status = 'Активен'"),
                    meterCount = db.scalarInt("SELECT COUNT(*) FROM meters"),
                    debtTotal = db.scalarDouble("SELECT IFNULL(SUM(balance), 0) FROM personal_accounts WHERE balance < 0"),
                    readingsThisMonth = db.scalarInt("SELECT COUNT(*) FROM meter_readings WHERE substr(reading_date, 1, 7) = strftime('%Y-%m', 'now')"),
                    verificationsSoon = db.scalarInt("SELECT COUNT(*) FROM meters WHERE date(next_verification_date) <= date('now', '+30 day')")
                ),
                customers = db.customers(query),
                accounts = db.accounts(query),
                meters = db.meters(query),
                readings = db.latestReadings(),
                payments = db.payments(query),
                query = query
            )
        } finally {
            db.close()
        }
    }

    suspend fun meterReadings(meterId: Long): List<MeterReading> = withContext(Dispatchers.IO) {
        val db = database.readableDatabase()
        try {
            db.rawQuery(
                """
                SELECT reading_id, meter_id, reading_date, value, method, status
                FROM meter_readings
                WHERE meter_id = ?
                ORDER BY reading_date DESC, reading_id DESC
                """.trimIndent(),
                arrayOf(meterId.toString())
            ).useRows(::readingFromCursor)
        } finally {
            db.close()
        }
    }

    suspend fun addReading(meterId: Long, date: String, value: Double, method: String) = withContext(Dispatchers.IO) {
        val db = database.readableDatabase()
        try {
            db.insert(
                "meter_readings",
                null,
                ContentValues().apply {
                    put("meter_id", meterId)
                    put("reading_date", date)
                    put("value", value)
                    put("method", method)
                    put("status", "Ожидает проверки")
                }
            )
        } finally {
            db.close()
        }
    }

    suspend fun addPayment(accountId: Long, date: String, amount: Double, method: String) = withContext(Dispatchers.IO) {
        val db = database.readableDatabase()
        try {
            db.beginTransaction()
            db.insert(
                "payments",
                null,
                ContentValues().apply {
                    put("account_id", accountId)
                    put("payment_date", date)
                    put("amount", amount)
                    put("method", method)
                    put("status", "Зачислен")
                }
            )
            db.execSQL(
                "UPDATE personal_accounts SET balance = balance + ? WHERE account_id = ?",
                arrayOf<Any>(amount, accountId)
            )
            db.setTransactionSuccessful()
        } finally {
            if (db.inTransaction()) db.endTransaction()
            db.close()
        }
    }
}

private fun SQLiteDatabase.customers(query: String): List<Customer> =
    rawQuery(
        """
        SELECT customer_id, last_name, first_name, middle_name, phone, email, region
        FROM customers
        WHERE ? = ''
           OR last_name LIKE '%' || ? || '%'
           OR first_name LIKE '%' || ? || '%'
           OR phone LIKE '%' || ? || '%'
           OR region LIKE '%' || ? || '%'
        ORDER BY last_name, first_name
        """.trimIndent(),
        arrayOf(query, query, query, query, query)
    ).useRows { cursor ->
        Customer(
            id = cursor.long("customer_id"),
            lastName = cursor.string("last_name"),
            firstName = cursor.string("first_name"),
            middleName = cursor.nullableString("middle_name"),
            phone = cursor.nullableString("phone"),
            email = cursor.nullableString("email"),
            region = cursor.string("region")
        )
    }

private fun SQLiteDatabase.accounts(query: String): List<PersonalAccount> =
    rawQuery(
        """
        SELECT personal_accounts.account_id, personal_accounts.account_number,
               personal_accounts.customer_id,
               customers.last_name || ' ' || customers.first_name || ' ' || IFNULL(customers.middle_name, '') AS customer_name,
               addresses.city || ', ' || addresses.street || ', ' || addresses.house ||
                   CASE WHEN addresses.apartment IS NULL THEN '' ELSE ', кв. ' || addresses.apartment END AS address,
               personal_accounts.service_type, personal_accounts.balance, personal_accounts.status
        FROM personal_accounts
        INNER JOIN customers ON customers.customer_id = personal_accounts.customer_id
        INNER JOIN addresses ON addresses.address_id = personal_accounts.address_id
        WHERE ? = ''
           OR personal_accounts.account_number LIKE '%' || ? || '%'
           OR customers.last_name LIKE '%' || ? || '%'
           OR addresses.city LIKE '%' || ? || '%'
           OR addresses.street LIKE '%' || ? || '%'
           OR personal_accounts.service_type LIKE '%' || ? || '%'
        ORDER BY personal_accounts.status, personal_accounts.account_number
        """.trimIndent(),
        arrayOf(query, query, query, query, query, query)
    ).useRows { cursor ->
        PersonalAccount(
            id = cursor.long("account_id"),
            number = cursor.string("account_number"),
            customerId = cursor.long("customer_id"),
            customerName = cursor.string("customer_name").trim(),
            address = cursor.string("address"),
            serviceType = cursor.string("service_type"),
            balance = cursor.double("balance"),
            status = cursor.string("status")
        )
    }

private fun SQLiteDatabase.meters(query: String): List<Meter> =
    rawQuery(
        """
        SELECT meters.meter_id, meters.account_id, personal_accounts.account_number,
               meters.serial_number, meters.type, meters.location, meters.next_verification_date,
               (
                   SELECT value FROM meter_readings
                   WHERE meter_readings.meter_id = meters.meter_id
                   ORDER BY reading_date DESC, reading_id DESC
                   LIMIT 1
               ) AS last_reading
        FROM meters
        INNER JOIN personal_accounts ON personal_accounts.account_id = meters.account_id
        WHERE ? = ''
           OR meters.serial_number LIKE '%' || ? || '%'
           OR meters.type LIKE '%' || ? || '%'
           OR meters.location LIKE '%' || ? || '%'
           OR personal_accounts.account_number LIKE '%' || ? || '%'
        ORDER BY date(meters.next_verification_date), meters.type
        """.trimIndent(),
        arrayOf(query, query, query, query, query)
    ).useRows { cursor ->
        Meter(
            id = cursor.long("meter_id"),
            accountId = cursor.long("account_id"),
            accountNumber = cursor.string("account_number"),
            serialNumber = cursor.string("serial_number"),
            type = cursor.string("type"),
            location = cursor.string("location"),
            nextVerificationDate = cursor.string("next_verification_date"),
            lastReading = cursor.nullableDouble("last_reading")
        )
    }

private fun SQLiteDatabase.latestReadings(): List<MeterReading> =
    rawQuery(
        """
        SELECT reading_id, meter_id, reading_date, value, method, status
        FROM meter_readings
        ORDER BY reading_date DESC, reading_id DESC
        LIMIT 20
        """.trimIndent(),
        emptyArray()
    ).useRows(::readingFromCursor)

private fun SQLiteDatabase.payments(query: String): List<Payment> =
    rawQuery(
        """
        SELECT payments.payment_id, personal_accounts.account_number,
               payments.payment_date, payments.amount, payments.method, payments.status
        FROM payments
        INNER JOIN personal_accounts ON personal_accounts.account_id = payments.account_id
        WHERE ? = ''
           OR personal_accounts.account_number LIKE '%' || ? || '%'
           OR payments.method LIKE '%' || ? || '%'
           OR payments.status LIKE '%' || ? || '%'
        ORDER BY payments.payment_date DESC, payments.payment_id DESC
        """.trimIndent(),
        arrayOf(query, query, query, query)
    ).useRows { cursor ->
        Payment(
            id = cursor.long("payment_id"),
            accountNumber = cursor.string("account_number"),
            paymentDate = cursor.string("payment_date"),
            amount = cursor.double("amount"),
            method = cursor.string("method"),
            status = cursor.string("status")
        )
    }

private fun readingFromCursor(cursor: Cursor): MeterReading =
    MeterReading(
        id = cursor.long("reading_id"),
        meterId = cursor.long("meter_id"),
        readingDate = cursor.string("reading_date"),
        value = cursor.double("value"),
        method = cursor.string("method"),
        status = cursor.string("status")
    )

private fun SQLiteDatabase.scalarInt(sql: String): Int =
    rawQuery(sql, emptyArray()).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }

private fun SQLiteDatabase.scalarDouble(sql: String): Double =
    rawQuery(sql, emptyArray()).use { cursor -> if (cursor.moveToFirst()) cursor.getDouble(0) else 0.0 }

private inline fun <T> Cursor.useRows(mapper: (Cursor) -> T): List<T> =
    use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(mapper(cursor))
        }
    }

private fun Cursor.index(name: String): Int = getColumnIndexOrThrow(name)
private fun Cursor.string(name: String): String = getString(index(name))
private fun Cursor.nullableString(name: String): String? = if (isNull(index(name))) null else getString(index(name))
private fun Cursor.long(name: String): Long = getLong(index(name))
private fun Cursor.double(name: String): Double = getDouble(index(name))
private fun Cursor.nullableDouble(name: String): Double? = if (isNull(index(name))) null else getDouble(index(name))
