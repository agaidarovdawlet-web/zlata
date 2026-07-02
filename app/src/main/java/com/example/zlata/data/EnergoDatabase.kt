package com.example.zlata.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

class EnergoDatabase private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var instance: EnergoDatabase? = null

        fun get(context: Context): EnergoDatabase =
            instance ?: synchronized(this) {
                instance ?: EnergoDatabase(context.applicationContext).also { instance = it }
            }
    }

    fun readableDatabase(): SQLiteDatabase {
        val dbFile = context.getDatabasePath("energosbyt_plus.db")
        if (!dbFile.exists()) {
            copyDatabase(dbFile)
        }
        return SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
    }

    private fun copyDatabase(dbFile: File) {
        dbFile.parentFile?.mkdirs()
        context.assets.open("energosbyt_plus.db").use { input ->
            dbFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
